// lib/notify.dart
import 'dart:convert';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:http/http.dart' as http;

final FlutterLocalNotificationsPlugin flnp = FlutterLocalNotificationsPlugin();

// ★ 스프링 서버 포트 확인(8093이면 아래도 8093)
const String _base = 'http://10.0.2.2:8093';

// 채널 ID/이름은 고정값으로 씁니다.
const String _channelId = 'bnk_local';
const String _channelName = 'BNK Local Notifications';

Future<void> initLocalNotifications() async {
  // 아이콘은 런처 아이콘을 씁니다(문제 있으면 drawable 흰색 아이콘 만들어서 교체)
  const androidInit = AndroidInitializationSettings('@mipmap/ic_launcher');
  const init = InitializationSettings(android: androidInit);
  await flnp.initialize(init);

  // 채널은 첫 show 때 자동생성되지만, 안전하게 선생성
  final androidImpl = flnp.resolvePlatformSpecificImplementation<
      AndroidFlutterLocalNotificationsPlugin>();

  await androidImpl?.createNotificationChannel(const AndroidNotificationChannel(
    _channelId,
    _channelName,
    importance: Importance.high,
    // description은 생략 가능
  ));

  // Android 13+ 권한 확인 및 요청
  final enabled = await androidImpl?.areNotificationsEnabled() ?? false;
  if (!enabled) {
    final granted = await androidImpl?.requestNotificationsPermission() ?? false;
    // granted가 false면 시스템 설정에서 직접 켜야 함(아래 버튼으로 안내)
    print('notifications enabled? $granted');
  }
}

Future<void> showLocalNotification(String title, String body) async {
  const androidDetails = AndroidNotificationDetails(
    _channelId,
    _channelName,
    importance: Importance.high,
    priority: Priority.high,
    // 필요시 아이콘 지정: icon: '@mipmap/ic_launcher',
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
    print('inbox status=${res.statusCode}');
    if (res.statusCode != 200) return;

    final List items = jsonDecode(res.body) as List;
    print('polled items: ${items.length}');
    if (items.isEmpty) return;

    final ids = <int>[];
    for (final raw in items) {
      await showLocalNotification(
        (raw['title'] ?? '알림').toString(),
        (raw['body'] ?? '').toString(),
      );
      ids.add(raw['id'] as int);
    }

    final ackRes = await http.post(
      Uri.parse('$_base/api/app/inbox/ack?uid=$uid'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode(ids),
    );
    print('ack status: ${ackRes.statusCode}');
  } catch (e) {
    print('fetchAndNotify error: $e');
  }
  Future<bool> ensureNotificationPermission() async {
    final android = flnp.resolvePlatformSpecificImplementation<
        AndroidFlutterLocalNotificationsPlugin>();
    // Android 13+에서만 의미 있음. 이하 버전은 true 취급
    final enabled = await android?.areNotificationsEnabled() ?? true;
    if (enabled) return true;
    final granted = await android?.requestNotificationsPermission() ?? false;
    return granted;
  }
}
