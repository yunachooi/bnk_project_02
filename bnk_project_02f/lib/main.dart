import 'package:flutter/material.dart';
import 'notify.dart';
import 'login/signin.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initLocalNotifications(); // ← 권한 체크/요청 포함
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});
  @override
  Widget build(BuildContext context) {
    return const MaterialApp(
      debugShowCheckedModeBanner: false,
      home: SignInWebViewPage(), // ← 로그인 WebView
    );
  }
}
