import 'package:flutter/material.dart';
import 'package:kakao_flutter_sdk_common/kakao_flutter_sdk_common.dart';
import 'login/signin.dart'; // 로그인 웹뷰 페이지
import 'shopping/product.dart'; // ShoppingHomePage가 들어있는 파일
import 'account/accountMain.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();

  KakaoSdk.init(
    nativeAppKey: 'fb5bb650ccba4fbe35685bf0b2e2af4d',
    javaScriptAppKey: '4e72545fa8c0e4a849eb05d1806a638f',
  );

  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'BNK App',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(useMaterial3: true),
      // 처음엔 로그인 화면으로 시작
      initialRoute: '/signin',
      routes: {
        '/signin': (_) => const SignInWebViewPage(),
        '/shopping': (_) => const ShoppingHomePage(), // ← 쇼핑 홈 라우트 추가
        '/account': (_) => const AccountMainPage(),
      },
    );
  }
}
