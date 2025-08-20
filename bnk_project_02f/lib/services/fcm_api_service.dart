import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:device_info_plus/device_info_plus.dart';
import 'dart:io';

class FCMApiService {
  static const String baseUrl = 'http://10.0.2.2:8093';

  static Future<bool> registerToken(String userId, String fcmToken) async {
    try {
      String deviceType = Platform.isAndroid ? 'ANDROID' : 'IOS';
      String deviceId = await _getDeviceId();

      print('FCM 토큰 등록 시도: $userId');
      print('서버 URL: $baseUrl/user/fcm-token');

      final response = await http.post(
        Uri.parse('$baseUrl/user/fcm-token'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'uid': userId,
          'fcmToken': fcmToken,
          'deviceType': deviceType,
          'deviceId': deviceId,
        }),
      );

      print('FCM 토큰 등록 응답: ${response.statusCode}');
      print('응답 내용: ${response.body}');

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        print('FCM 토큰 등록 성공: ${data['message']}');
        return data['success'] ?? false;
      } else {
        print('FCM 토큰 등록 실패: ${response.body}');
      }

      return false;
    } catch (e) {
      print('FCM 토큰 등록 오류: $e');
      return false;
    }
  }

  static Future<bool> processPayment({
    required String uid,
    required String spno,
    required String cardno,
    required String amount,
    required String currency,
  }) async {
    try {
      print('결제 처리 시도: $amount원');

      final response = await http.post(
        Uri.parse('$baseUrl/user/shopping/log/saveLog'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'uid': uid,
          'spno': spno,
          'cardno': cardno,
          'slamount': amount,
          'slcurrency': currency,
        }),
      );

      print('결제 처리 응답: ${response.statusCode}');
      print('결제 처리 응답 내용: ${response.body}');

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        return data['success'] ?? false;
      }

      return false;
    } catch (e) {
      print('결제 처리 오류: $e');
      return false;
    }
  }

  static Future<bool> sendTestNotification(String userId, String message) async {
    try {
      print('테스트 알림 발송 시도');

      final response = await http.post(
        Uri.parse('$baseUrl/user/test-notification'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'uid': userId,
          'message': message,
        }),
      );

      print('테스트 알림 응답: ${response.statusCode}');

      return response.statusCode == 200;
    } catch (e) {
      print('테스트 알림 발송 오류: $e');
      return false;
    }
  }

  static Future<String> _getDeviceId() async {
    DeviceInfoPlugin deviceInfo = DeviceInfoPlugin();

    if (Platform.isAndroid) {
      AndroidDeviceInfo androidInfo = await deviceInfo.androidInfo;
      return androidInfo.id;
    } else if (Platform.isIOS) {
      IosDeviceInfo iosInfo = await deviceInfo.iosInfo;
      return iosInfo.identifierForVendor ?? 'unknown';
    }

    return 'unknown';
  }
}