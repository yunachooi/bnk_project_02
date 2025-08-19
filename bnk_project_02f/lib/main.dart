// lib/main.dart
import 'package:flutter/material.dart';
import 'package:bnk_project_02f/login/signin.dart';      // ✅ 로그인 화면
// (AccountMainPage는 로그인 후에 이동)
import 'package:bnk_project_02f/notify.dart'; // ⬅️ 추가

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initLocalNotifications();
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
      home: const SignInWebViewPage(),                  // ✅ 여기!
    );
  }
}
