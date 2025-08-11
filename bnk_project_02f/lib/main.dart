import 'package:flutter/material.dart';
import 'login/signin.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const BNKApp());
}

class BNKApp extends StatelessWidget {
  const BNKApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'BNK WebView',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(useMaterial3: true),
      home: const SignInWebViewPage(),
    );
  }
}