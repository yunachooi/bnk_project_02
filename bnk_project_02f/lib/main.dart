import 'package:flutter/material.dart';
import 'package:bnk_project_02f/login/signin.dart';
import 'package:bnk_project_02f/notify.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:bnk_project_02f/services/push_notification_service.dart';
import 'package:bnk_project_02f/account/accountMain.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Firebase.initializeApp();
  await initLocalNotifications();
  await PushNotificationService.initialize();

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