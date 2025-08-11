import 'package:flutter/material.dart';
import 'package:webview_flutter/webview_flutter.dart';

class SignUpWebViewPage extends StatefulWidget {
  const SignUpWebViewPage({super.key});

  @override
  State<SignUpWebViewPage> createState() => _SignUpWebViewPageState();
}

class _SignUpWebViewPageState extends State<SignUpWebViewPage> {
  late final WebViewController _controller;
  double _progress = 0;

  static const String _signupUrl = 'http://10.0.2.2:8093/user/signup';

  @override
  void initState() {
    super.initState();

    _controller = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setBackgroundColor(Colors.transparent)
      ..setNavigationDelegate(
        NavigationDelegate(
          onNavigationRequest: (req) => NavigationDecision.navigate,
          onProgress: (p) => setState(() => _progress = p / 100),
        ),
      )
      ..loadRequest(Uri.parse(_signupUrl));
  }

  Future<bool> _onWillPop() async {
    if (await _controller.canGoBack()) {
      await _controller.goBack();
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
          title: const Text('회원가입'),
          actions: [
            IconButton(
              tooltip: '새로고침',
              onPressed: () => _controller.reload(),
              icon: const Icon(Icons.refresh),
            ),
          ],
          bottom: PreferredSize(
            preferredSize: const Size.fromHeight(2),
            child: _progress < 1
                ? LinearProgressIndicator(value: _progress)
                : const SizedBox.shrink(),
          ),
        ),
        body: RefreshIndicator(
          onRefresh: () async => _controller.reload(),
          child: WebViewWidget(controller: _controller),
        ),
      ),
    );
  }
}