import 'dart:io' show Platform;
import 'package:flutter/material.dart';
import 'package:webview_flutter/webview_flutter.dart';
import 'package:url_launcher/url_launcher.dart';

// ✅ 쇼핑 네이티브 화면 (product.dart 안의 전체 앱)을 푸시해서 열어요.
import 'package:bnk_project_02f/shopping/product.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'BNK WebView',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(useMaterial3: true),
      home: const AccountMainPage(),
    );
  }
}

class AccountMainPage extends StatefulWidget {
  const AccountMainPage({super.key});

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
    final startUrl = '${_baseUrl()}/foreign0';

    _controller = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setBackgroundColor(const Color(0x00000000))
    // ✅ JS 채널 추가: 페이지에서 BNK.postMessage('goShopping') 호출 시 네이티브로 전환
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

            // 1) 커스텀 스킴 fallback(bnk://shopping)도 지원
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
            setState(() => _isLoading = false);
            // ✅ “해외직구” 버튼 클릭 가로채서 BNK.postMessage('goShopping') 호출하도록 주입
            await _injectHookScript();
          },
        ),
      )
      ..loadRequest(Uri.parse(startUrl));
  }

  /// 네이티브 쇼핑 화면으로 전환
  void _goNativeShopping() {
    if (_pushedShopping || !mounted) return;
    _pushedShopping = true;
    Navigator.of(context).push(
      MaterialPageRoute(builder: (_) => ShoppingApp()),
    ).then((_) {
      // 돌아오면 다시 전환 가능하도록 플래그 해제
      _pushedShopping = false;
    });
  }

  /// “해외직구” 버튼 클릭을 감지/가로채는 스크립트 주입
  Future<void> _injectHookScript() async {
    const js = r'''
(function() {
  if (window.__BNK_HOOKED__) return;
  window.__BNK_HOOKED__ = true;

  function tryAttach(el) {
    if (!el || el.__bnkHooked) return;
    const text = (el.innerText || '').replace(/\s+/g,'').trim();
    // 버튼 텍스트에 "해외직구" 포함되면 가로채기
    if (text.includes('해외직구')) {
      el.__bnkHooked = true;
      el.addEventListener('click', function(e) {
        try {
          e.preventDefault();
          e.stopPropagation();
        } catch (_) {}
        // 1) JS 채널로 네이티브 호출
        try { BNK.postMessage('goShopping'); } catch (err) {
          // 2) 채널이 막힌 경우 커스텀 스킴으로 fallback
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

  // 초기 스캔
  scan();

  // 동적 변경 대응
  try {
    var obs = new MutationObserver(scan);
    obs.observe(document.documentElement, {childList:true, subtree:true});
  } catch (_) {}
})();
''';
    try {
      await _controller.runJavaScript(js);
    } catch (_) {
      // 주입 실패는 무시(일부 페이지 CSP 등)
    }
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
          title: const Text('BNK 쇼핑환전'),
          actions: [
            IconButton(onPressed: _reload, icon: const Icon(Icons.refresh)),
          ],
        ),
        body: Stack(
          children: [
            WebViewWidget(controller: _controller),
            if (_isLoading) const LinearProgressIndicator(minHeight: 2),
          ],
        ),
      ),
    );
  }
}