// lib/login/signin.dart
import 'dart:io' show Platform;
import 'package:flutter/material.dart';
import 'package:webview_flutter/webview_flutter.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:bnk_project_02f/account/accountMain.dart';
import 'package:bnk_project_02f/notify.dart';
import 'package:bnk_project_02f/polling_manager.dart';
// FCM 관련 import 추가
import 'package:bnk_project_02f/services/push_notification_service.dart';
import 'package:bnk_project_02f/services/fcm_api_service.dart';

class SignInWebViewPage extends StatefulWidget {
  const SignInWebViewPage({super.key});

  @override
  State<SignInWebViewPage> createState() => _SignInWebViewPageState();
}

class _SignInWebViewPageState extends State<SignInWebViewPage> {
  late final WebViewController _controller;

  String _host() => Platform.isAndroid ? '10.0.2.2' : 'localhost';
  String get _base => 'http://${_host()}:8093';
  Uri get _loginUri => Uri.parse('$_base/user/login');
  Uri get _foreignUri => Uri.parse('$_base/foreign');

  bool _navigatedToNative = false;
  bool _loading = false;
  bool _needsHostFix(Uri u) =>
      (u.host == 'localhost' || u.host == '127.0.0.1') && (u.scheme == 'http' || u.scheme == 'https');

  Uri _fixHost(Uri u) => u.replace(host: _host()); // _host()는 이미 10.0.2.2/localhost 반환

  // ✅ 서버가 userhome/usermain으로 보내도 foreign으로 한 번만 강제 리다이렉트
  bool _forcedToForeign = false;

  @override
  void initState() {
    super.initState();

    _controller = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setBackgroundColor(Colors.white)

    // WebView 안에서 UID -> Flutter
      ..addJavaScriptChannel(
        'BNKUID',
        onMessageReceived: (JavaScriptMessage msg) async {
          final uid = msg.message.trim();
          if (uid.isEmpty || !mounted) return;
          await _handleUid(uid);
        },
      )

      ..setNavigationDelegate(
        NavigationDelegate(
          onPageStarted: (_) {
            if (mounted) setState(() => _loading = true); // mounted 가드
          },
          onPageFinished: (_) async {
            if (!mounted) return;                          // mounted 가드
            setState(() => _loading = false);

            final current = await _controller.currentUrl();
            if (current == null) return;
            final uri = Uri.parse(current);

            // host가 localhost/127.0.0.1로 로드됐으면 10.0.2.2로 교체 재로딩
            if (_needsHostFix(uri)) {
              final fixed = _fixHost(uri);
              await _controller.loadRequest(fixed);
              return;
            }

            // userhome/usermain이면 foreign으로 강제 (1회)
            if (_isUserHomeStyle(uri) && !_forcedToForeign) {
              _forcedToForeign = true;
              await _controller.loadRequest(_foreignUri);
              return;
            }

            // foreign에 도달하면: UID 브릿지 주입만 하고, BNKUID 도착할 때까지 대기
            if (_isForeign(uri)) {
              await _injectUidBridge();
              // 1.5초 안 오면 백업으로 네이티브 전환
              Future.delayed(const Duration(milliseconds: 1500), () {
                if (!_navigatedToNative && mounted) {
                  _navigateToAccount(null);
                }
              });
            }
          },
          onNavigationRequest: (request) {
            final uri = Uri.parse(request.url);

            // 네비게이션 단계에서도 host 교정
            if (_needsHostFix(uri)) {
              _controller.loadRequest(_fixHost(uri));
              return NavigationDecision.prevent;
            }

            // userhome/usermain → foreign 강제
            if (_isUserHomeStyle(uri) && !_forcedToForeign) {
              _forcedToForeign = true;
              _controller.loadRequest(_foreignUri);
              return NavigationDecision.prevent;
            }

            return NavigationDecision.navigate;
          },
        ),
      )
      ..loadRequest(_loginUri);
  }

  // ✅ 성공 페이지는 foreign만 인정
  bool _isForeign(Uri uri) {
    final p = uri.path.toLowerCase();
    return p == '/foreign' ||
        p.endsWith('/foreign') ||
        uri.pathSegments.any((s) => s.toLowerCase() == 'foreign');
  }

  bool _isUserHomeStyle(Uri uri) {
    final p = uri.path.toLowerCase();
    return p == '/userhome' ||
        p.endsWith('/userhome') ||
        p.endsWith('/user/usermain') ||
        p.endsWith('/usermain') ||
        uri.pathSegments.any((s) =>
        s.toLowerCase() == 'userhome' || s.toLowerCase() == 'usermain');
  }

  bool _maybeGoToAccount(Uri uri) {
    if (_navigatedToNative) return true;
    if (!_isForeign(uri)) return false;

    _navigatedToNative = true;

    WidgetsBinding.instance.addPostFrameCallback((_) async {
      if (!mounted) return;

      String? uid = uri.queryParameters['uid']?.trim();
      if (uid != null && uid.isEmpty) uid = null;

      if (uid != null) {
        final prefs = await SharedPreferences.getInstance();
        await prefs.setString('uid', uid);
      }

      await ensureNotificationPermission();

      if (uid != null && !PollingManager.isRunning) {
        PollingManager.start(uid);
      }
      if (uid != null) {
        await fetchAndNotify(uid);
      }

      if (uid != null) {
        await _registerFCMToken(uid);
      }

      if (!mounted) return;
      Navigator.of(context).pushAndRemoveUntil(
        MaterialPageRoute(builder: (_) => AccountMainPage(uid: uid)),
            (route) => false,
      );
    });
    return true;
  }

  Future<void> _injectUidBridge() async {
    const js = r"""
(async function() {
  if (window.__BNK_UID_SENT__) return;

  function send(uid) {
    if (!uid) return;
    try { BNKUID.postMessage(String(uid)); window.__BNK_UID_SENT__ = true; } catch(e) {}
  }

  try {
    const r = await fetch('/api/app/me', { credentials: 'include' });
    if (r.ok) {
      const j = await r.json().catch(()=>null);
      if (j && j.uid) { send(j.uid); return; }
    }
  } catch(e) {}

  try {
    var el = document.querySelector('[data-uid], #uid, .uid, meta[name="uid"]');
    var v = '';
    if (el) {
      v = (el.getAttribute && (el.getAttribute('data-uid') || el.getAttribute('content'))) ||
          (el.textContent || '');
      v = (v || '').trim();
      if (v) { send(v); return; }
    }
  } catch(e) {}

  try {
    var m = document.cookie.match(/(?:^|;\s*)uid=([^;]+)/);
    if (m && m[1]) { send(decodeURIComponent(m[1])); return; }
  } catch(e) {}
})();
""";
    try { await _controller.runJavaScript(js); } catch (_) {}
  }

  // UID 수신 → 저장 → 권한 → 폴링 → 즉시 fetch → FCM 토큰 등록 → 네이티브 전환
  Future<void> _handleUid(String uid) async {
    debugPrint('[signin] BNKUID=$uid');
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('uid', uid);

    await ensureNotificationPermission();
    if (!PollingManager.isRunning) {
      PollingManager.start(uid);
    }
    await fetchAndNotify(uid);

    await _registerFCMToken(uid);

    _navigateToAccount(uid);
  }

  Future<void> _registerFCMToken(String uid) async {
    try {
      debugPrint('[FCM] 토큰 등록 시작: $uid');

      String? token = await PushNotificationService.getToken();
      if (token != null) {
        bool success = await FCMApiService.registerToken(uid, token);
        if (success) {
          debugPrint('[FCM] 토큰 등록 성공: $uid');
        } else {
          debugPrint('[FCM] 토큰 등록 실패: $uid');
        }
      } else {
        debugPrint('[FCM] FCM 토큰을 가져올 수 없음');
      }
    } catch (e) {
      debugPrint('[FCM] 토큰 등록 중 오류: $e');
    }
  }

  void _navigateToAccount(String? uid) {
    if (_navigatedToNative || !mounted) return;
    _navigatedToNative = true;
    Navigator.of(context).pushAndRemoveUntil(
      MaterialPageRoute(builder: (_) => AccountMainPage(uid: uid)),
          (route) => false,
    );
  }

  Future<void> _reload() async {
    try { await _controller.reload(); } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('로그인'),
        actions: [
          IconButton(icon: const Icon(Icons.refresh), onPressed: _reload, tooltip: '새로고침'),
        ],
        bottom: _loading
            ? const PreferredSize(
          preferredSize: Size.fromHeight(2),
          child: LinearProgressIndicator(minHeight: 2),
        )
            : null,
      ),
      body: PopScope(
        canPop: false,
        onPopInvoked: (didPop) async {
          if (didPop) return;
          if (await _controller.canGoBack()) {
            _controller.goBack();
          } else {
            if (context.mounted) Navigator.of(context).maybePop();
          }
        },
        child: WebViewWidget(controller: _controller),
      ),
    );
  }
}