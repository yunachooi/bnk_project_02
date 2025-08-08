import 'package:flutter/material.dart';
import 'login/signin.dart';
import 'login/signup.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'BNK Project',
      initialRoute: '/signin',
      routes: {
        '/signin': (_) => const SignInPage(),
        '/signup': (_) => const SignUpPage(),
      },
      theme: ThemeData(useMaterial3: true),
    );
  }
}