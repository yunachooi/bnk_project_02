import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:webview_flutter/webview_flutter.dart';
import 'package:url_launcher/url_launcher.dart';

class BranchMapPage extends StatefulWidget {
  const BranchMapPage({super.key});
  @override
  State<BranchMapPage> createState() => _BranchMapPageState();
}

class _BranchMapPageState extends State<BranchMapPage> {
  late final WebViewController _controller;

  @override
  void initState() {
    super.initState();

    _controller = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setBackgroundColor(Colors.white)
    // Web → Flutter 브리지 (HTML에서 window.NAVER.postMessage(...) 호출)
      ..addJavaScriptChannel(
        'NAVER',
        onMessageReceived: (msg) async {
          // { type:'openUrl', url:'nmap://...', fallback:'https://...' }
          final Map<String, dynamic> data = jsonDecode(msg.message);
          final String? nmap = data['url'] as String?;
          final String? web = data['fallback'] as String?;
          await _openRouteUrl(nmap, web);
        },
      )
      ..setNavigationDelegate(
        NavigationDelegate(
          // 페이지 로드 마다 인터셉터 JS 주입
          onPageFinished: (url) async {
            await _injectSchemeClickInterceptor();
          },
          // 모든 네비게이션을 검문 — 최후의 방어선
          onNavigationRequest: (req) {
            final u = req.url;
            if (u.startsWith('nmap://') || u.startsWith('intent://')) {
              final fixed = u.startsWith('intent://')
                  ? u.replaceFirst('intent://', 'nmap://').split('#Intent').first
                  : u;
              _openRouteUrl(fixed, null);
              return NavigationDecision.prevent;
            }
            return NavigationDecision.navigate;
          },
        ),
      )
    // 당신 서버 주소
      ..loadRequest(Uri.parse('http://10.0.2.2:8093/branches'));
  }

  /// HTML 내부의 <a href="nmap://...">, intent://, window.open 케이스까지 전부 가로채서
  /// NAVER 채널로 전달하는 자바스크립트를 주입한다.
  Future<void> _injectSchemeClickInterceptor() async {
    const js = r'''
      (function(){
        if (window.__BNK_NMAP_PATCHED__) return;
        window.__BNK_NMAP_PATCHED__ = true;

        function postToNaver(url, fallback){
          try{
            if (window.NAVER && typeof window.NAVER.postMessage === 'function'){
              window.NAVER.postMessage(JSON.stringify({ type:'openUrl', url:url, fallback:fallback || null }));
              return true;
            }
          }catch(_){}
          return false;
        }

        // 1) a[href^="nmap://"/"intent://"] 클릭 가로채기
        document.addEventListener('click', function(e){
          var a = e.target && e.target.closest ? e.target.closest('a[href^="nmap://"], a[href^="intent://"]') : null;
          if(!a) return;
          e.preventDefault();
          var href = a.getAttribute('href') || '';
          if (href.indexOf('intent://') === 0) {
            href = href.replace('intent://', 'nmap://').split('#Intent')[0];
          }
          var fallback = a.getAttribute('data-fallback') || null;
          if (!postToNaver(href, fallback)) {
            // 브라우저에서 열린 경우: 앱 시도 → 실패 시 웹 폴백
            var timer = setTimeout(function(){ if(fallback) window.open(fallback, '_blank'); }, 700);
            location.href = href;
            window.addEventListener('pagehide', function(){ clearTimeout(timer); }, {once:true});
          }
        }, true);

        // 2) window.postMessage로 들어오는 (openUrl) 처리 (보수용)
        window.addEventListener('message', function(e){
          try{
            var data = typeof e.data === 'string' ? JSON.parse(e.data) : e.data;
            if (data && data.type === 'openUrl' && String(data.url||'').indexOf('nmap://') === 0) {
              postToNaver(data.url, data.fallback || null);
            }
          }catch(_){}
        });
      })();
    ''';

    try {
      await _controller.runJavaScript(js);
    } catch (_) {
      // 주입 실패해도 onNavigationRequest가 막아줌
    }
  }

  /// 외부 앱(네이버지도) or 폴백으로 안전하게 열기
  Future<void> _openRouteUrl(String? nmapUrl, String? webFallback) async {
    // 1) 네이버 지도 앱(nmap://) 시도
    if (nmapUrl != null && nmapUrl.isNotEmpty) {
      final uri = Uri.parse(nmapUrl);
      if (await canLaunchUrl(uri)) {
        await launchUrl(uri, mode: LaunchMode.externalApplication);
        return;
      }
    }

    // 2) 앱이 없으면 플레이스토어 이동 (없는 에뮬레이터도 있으니 try-catch)
    try {
      final market = Uri.parse('market://details?id=com.nhn.android.nmap');
      if (await canLaunchUrl(market)) {
        await launchUrl(market, mode: LaunchMode.externalApplication);
        return;
      }
    } catch (_) {}

    // 3) 마지막 폴백: 웹 길찾기
    if (webFallback != null && webFallback.isNotEmpty) {
      final uri = Uri.parse(webFallback);
      await launchUrl(uri, mode: LaunchMode.externalApplication);
      return;
    }

    // 4) 그래도 안되면 안내
    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('네이버 지도를 열 수 없습니다.')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('BNK 쇼핑환전')),
      body: WebViewWidget(controller: _controller),
    );
  }
}
