import 'dart:async';
import 'notify.dart';

class PollingManager {
  static Timer? _timer;
  static String? _uid;

  static bool get isRunning => _timer != null;

  static void start(
    String uid, {
    Duration interval = const Duration(seconds: 5),
  }) {
    _uid = uid;
    _timer?.cancel();
    _timer = Timer.periodic(interval, (_) => fetchAndNotify(uid));
  }

  static void stop() {
    _timer?.cancel();
    _timer = null;
    _uid = null;
  }
}
