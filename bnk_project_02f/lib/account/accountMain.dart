// lib/account/accountMain.dart
import 'dart:io' show Platform;
import 'package:flutter/material.dart';
import 'package:webview_flutter/webview_flutter.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:bnk_project_02f/shopping/product.dart';
import 'package:bnk_project_02f/notify.dart';
import 'package:bnk_project_02f/polling_manager.dart';

class AccountMainPage extends StatefulWidget {
  final String? uid; // ← SignIn에서 전달 (없으면 null)
  const AccountMainPage({super.key, this.uid});

  @override
  State<AccountMainPage> createState() => _AccountMainPageState();
}

class _AccountMainPageState extends State<AccountMainPage> {
  late final WebViewController _controller;
  bool _isLoading = true;
  bool _pushedShopping = false; // 네이티브 전환 중복 방지

  String _host() => Platform.isAndroid ? '10.0.2.2' : 'localhost';
  String _baseUrl() => 'http://${_host()}:8093';

  @override
  void initState() {
    super.initState();
    _startPollingBackup(); // ← 로그인 감지 실패해도 보장

    final startUrl = '${_baseUrl()}/foreign';
    _controller = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setBackgroundColor(const Color(0x00000000))
      ..addJavaScriptChannel(
        'BNK',
        onMessageReceived: (JavaScriptMessage msg) {
          if (msg.message == 'goShopping') {
            _goNativeShopping();
          }
        },
      )
      ..setNavigationDelegate(
        NavigationDelegate(
          onNavigationRequest: (req) async {
            final uri = Uri.parse(req.url);

            // 1) 커스텀 스킴 fallback(bnk://shopping)
            if (uri.scheme.toLowerCase() == 'bnk' &&
                uri.host.toLowerCase() == 'shopping') {
              _goNativeShopping();
              return NavigationDecision.prevent;
            }

            // 2) 같은 오리진 + /shopping 포함 주소도 네이티브로
            final base = Uri.parse(_baseUrl());
            final sameOrigin = (uri.host == base.host) && (uri.port == base.port);
            if (sameOrigin && uri.path.toLowerCase().contains('/shopping')) {
              _goNativeShopping();
              return NavigationDecision.prevent;
            }

            // 3) tel/mailto/intent 외부 스킴 처리
            final url = req.url;
            if (url.startsWith('tel:') ||
                url.startsWith('mailto:') ||
                url.startsWith('intent:')) {
              final launchUri = Uri.parse(url);
              if (await canLaunchUrl(launchUri)) {
                await launchUrl(launchUri);
              }
              return NavigationDecision.prevent;
            }

            return NavigationDecision.navigate;
          },
          onPageStarted: (_) => setState(() => _isLoading = true),
          onPageFinished: (_) async {
            if (!mounted) return;
            setState(() => _isLoading = false);
            await _injectHookScript(); // “해외직구” 버튼 가로채기
          },
        ),
      )
      ..loadRequest(Uri.parse(startUrl));
  }

  /// 로그인 감지 실패해도 UID 기반 폴링을 보장하는 백업
  Future<void> _startPollingBackup() async {
    String? uid = widget.uid;

    if (uid == null || uid.isEmpty) {
      final prefs = await SharedPreferences.getInstance();
      uid = prefs.getString('uid');
    }
    if (uid == null || uid.isEmpty) {
      debugPrint('[polling] uid 없음 → 폴링 생략');
      return;
    }

    await ensureNotificationPermission();

    if (!PollingManager.isRunning) {
      PollingManager.start(uid);
    }
    await fetchAndNotify(uid); // 진입 즉시 한 번
  }

  void _goNativeShopping() {
    if (_pushedShopping || !mounted) return;
    _pushedShopping = true;
    Navigator.of(context).push(
      MaterialPageRoute(builder: (_) => const ShoppingApp()),
    ).then((_) => _pushedShopping = false);
  }

  Future<void> _injectHookScript() async {
    const js = r'''
(function() {
  if (window.__BNK_HOOKED__) return;
  window.__BNK_HOOKED__ = true;

  function tryAttach(el) {
    if (!el || el.__bnkHooked) return;
    const text = (el.innerText || '').replace(/\s+/g,'').trim();
    if (text.includes('해외직구')) {
      el.__bnkHooked = true;
      el.addEventListener('click', function(e) {
        try { e.preventDefault(); e.stopPropagation(); } catch (_) {}
        try { BNK.postMessage('goShopping'); } catch (err) {
          try { window.location.href = 'bnk://shopping'; } catch (_) {}
        }
        return false;
      }, true);
    }
  }

  function scan() {
    try {
      var nodes = document.querySelectorAll('a,button,[role="button"],.btn');
      for (var i=0; i<nodes.length; i++) tryAttach(nodes[i]);
    } catch (_) {}
  }

  scan();
  try {
    var obs = new MutationObserver(scan);
    obs.observe(document.documentElement, {childList:true, subtree:true});
  } catch (_) {}
})();
''';
    try {
      await _controller.runJavaScript(js);
    } catch (_) {}
  }

  Future<void> _reload() async => _controller.reload();

  Future<bool> _onWillPop() async {
    if (await _controller.canGoBack()) {
      _controller.goBack();
      return false;
    }
    return true;
  }

  @override
  Widget build(BuildContext context) {
    return WillPopScope(
      onWillPop: _onWillPop,
      child: Scaffold(
        appBar: AppBar(
          backgroundColor: Colors.white,
          elevation: 1,
          shadowColor: Colors.grey.withOpacity(0.3),
          leading: IconButton(
            icon: const Icon(Icons.home, color: Colors.black),
            onPressed: () {
              Navigator.of(context).pushReplacement(
                MaterialPageRoute(
                  builder: (_) => AccountMainPage(uid: widget.uid),
                ),
              );
            },
          ),
          automaticallyImplyLeading: false, // 기본 뒤로가기 버튼 제거
          actions: [
            IconButton(
              icon: const Icon(Icons.refresh, color: Colors.black),
              onPressed: _reload,
            ),
          ],
        ),
        body: Stack(
          children: [
            WebViewWidget(controller: _controller),
            if (_isLoading) const LinearProgressIndicator(minHeight: 2),
          ],
        ),

        // ▼ 하단 네비게이터: 아이콘만, 가운데 정렬(기존 Account 스타일)
        bottomNavigationBar: SafeArea(
          top: false,
          child: BottomAppBar(
            color: Colors.white,
            elevation: 8,
            child: SizedBox(
              height: 56,
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceAround,
                children: [
                  // ① 해외직구 (네이티브 쇼핑앱 진입)
                  IconButton(
                    tooltip: '해외직구',
                    icon: const Icon(Icons.shopping_bag_outlined, color: Colors.black),
                    onPressed: _goNativeShopping,
                  ),

                  // ② accountMain (현재 페이지를 새로 띄워 루트로 복귀/리셋)
                  IconButton(
                    tooltip: 'accountMain',
                    icon: const Icon(Icons.home_outlined, color: Colors.black),
                    onPressed: () {
                      Navigator.of(context).pushReplacement(
                        MaterialPageRoute(
                          builder: (_) => AccountMainPage(uid: widget.uid),
                        ),
                      );
                    },
                  ),

                  // ③ 환전 메인 (웹뷰를 /foreign 으로 이동)
                  IconButton(
                    tooltip: '환전 메인',
                    icon: const Icon(Icons.currency_exchange, color: Colors.black),
                    onPressed: () {
                      _controller.loadRequest(Uri.parse('${_baseUrl()}/forex'));
                    },
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}