import 'package:flutter/material.dart';
import 'package:webview_flutter/webview_flutter.dart';
import 'package:bnk_project_02f/account/accountMain.dart';

class SignInWebViewPage extends StatefulWidget {
  const SignInWebViewPage({super.key});

  @override
  State<SignInWebViewPage> createState() => _SignInWebViewPageState();
}

class _SignInWebViewPageState extends State<SignInWebViewPage> {
  late final WebViewController _controller;

  // 로그인 시작 URL (환경에 맞게)
  final String _loginUrl = 'http://10.0.2.2:8093/user/login';

  bool _navigatedToNative = false; // 네이티브 전환 중복 방지
  bool _loading = false;           // 로딩 인디케이터

  @override
  void initState() {
    super.initState();

    _controller = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setBackgroundColor(Colors.white)
      ..setNavigationDelegate(
        NavigationDelegate(
          onPageStarted: (url) {
            setState(() => _loading = true);
          },
          onPageFinished: (url) async {
            setState(() => _loading = false);
            // 혹시 리다이렉트가 onNavigationRequest에 안 잡히는 경우 대비
            final current = await _controller.currentUrl();
            if (current != null) _maybeGoToAccount(Uri.parse(current));
          },
          onNavigationRequest: (request) {
            final uri = Uri.parse(request.url);
            // 리다이렉트 감지
            if (_maybeGoToAccount(uri)) {
              return NavigationDecision.prevent; // WebView로의 이동은 막음
            }
            return NavigationDecision.navigate;
          },
        ),
      )
      ..loadRequest(Uri.parse(_loginUrl));
  }

  /// 서버가 로그인 성공 시 보내는 userMain 계열 경로를 감지하여
  /// 네이티브 AccountMainPage로 전환.
  bool _maybeGoToAccount(Uri uri) {
    if (_navigatedToNative) return true;

    // 경로 판별: /userhome, /user/userMain, /userMain 등을 모두 허용
    final p = uri.path.toLowerCase();
    final looksLikeUserHome =
        p == '/userhome' ||
            p.endsWith('/userhome') ||
            p.endsWith('/user/usermain') ||
            p.endsWith('/usermain') ||
            uri.pathSegments.any((s) => s.toLowerCase() == 'userhome' || s.toLowerCase() == 'usermain');

    if (looksLikeUserHome) {
      _navigatedToNative = true;

      // 프레임 끝에서 안전하게 네이게이션
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted) return;
        Navigator.of(context).pushAndRemoveUntil(
          MaterialPageRoute(builder: (_) => const AccountMainPage()),
              (route) => false,
        );
      });
      return true;
    }
    return false;
  }

  Future<void> _reload() async {
    try {
      await _controller.reload();
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('로그인'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _reload,
            tooltip: '새로고침',
          ),
        ],
        bottom: _loading
            ? const PreferredSize(
          preferredSize: Size.fromHeight(2),
          child: LinearProgressIndicator(minHeight: 2),
        )
            : null,
      ),
      // Android Predictive Back 대응: PopScope 사용
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