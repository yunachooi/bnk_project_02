// lib/notify.dart (수정본)
import 'dart:convert';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:http/http.dart' as http;

final FlutterLocalNotificationsPlugin flnp = FlutterLocalNotificationsPlugin();

// ★ 서버 주소 (에뮬레이터면 10.0.2.2)
const String _base = 'http://10.0.2.2:8093';

const String _channelId = 'bnk_local';
const String _channelName = 'BNK Local Notifications';

Future<void> initLocalNotifications() async {
  const androidInit = AndroidInitializationSettings('@mipmap/ic_launcher');
  const init = InitializationSettings(android: androidInit);
  await flnp.initialize(init);

  final android = flnp.resolvePlatformSpecificImplementation<
      AndroidFlutterLocalNotificationsPlugin>();
  await android?.createNotificationChannel(const AndroidNotificationChannel(
    _channelId, _channelName,
    importance: Importance.high,
  ));
}

Future<bool> ensureNotificationPermission() async {
  final android = flnp.resolvePlatformSpecificImplementation<
      AndroidFlutterLocalNotificationsPlugin>();
  final enabled = await android?.areNotificationsEnabled() ?? true;
  if (enabled) return true;
  return await android?.requestNotificationsPermission() ?? false;
}

Future<void> showLocalNotification(String title, String body) async {
  const androidDetails = AndroidNotificationDetails(
    _channelId, _channelName,
    importance: Importance.high,
    priority: Priority.high,
  );
  const details = NotificationDetails(android: androidDetails);

  await flnp.show(
    DateTime.now().millisecondsSinceEpoch ~/ 1000,
    title,
    body,
    details,
  );
}

Future<void> fetchAndNotify(String uid) async {
  try {
    final res = await http.get(Uri.parse('$_base/api/app/inbox?uid=$uid'));
    if (res.statusCode != 200) return;

    final List items = jsonDecode(res.body) as List;
    if (items.isEmpty) return;

    final ids = <int>[];
    for (final raw in items) {
      await showLocalNotification(
        (raw['title'] ?? '알림').toString(),
        (raw['body'] ?? '').toString(),
      );
      if (raw['id'] != null) ids.add(raw['id'] as int);
    }

    if (ids.isNotEmpty) {
      await http.post(
        Uri.parse('$_base/api/app/inbox/ack?uid=$uid'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode(ids),
      );
    }
  } catch (e) {
    print('fetchAndNotify error: $e');
  }
  print('[fetch] GET $_base/api/app/inbox?uid=$uid');
  final res = await http.get(Uri.parse('$_base/api/app/inbox?uid=$uid'));
  print('[fetch] status=${res.statusCode} body=${res.body}');
}