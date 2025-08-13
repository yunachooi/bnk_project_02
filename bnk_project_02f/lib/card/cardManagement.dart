import 'package:flutter/material.dart';
import 'package:dio/dio.dart';
import 'package:dio_cookie_manager/dio_cookie_manager.dart';
import 'package:cookie_jar/cookie_jar.dart';

void main() {
  runApp(MaterialApp(
    title: '카드 관리',
    theme: ThemeData(
      primarySwatch: Colors.blue,
      primaryColor: Color(0xFF1976D2),
    ),
    home: CardManagementScreen(),
    debugShowCheckedModeBanner: false,
  ));
}

class HttpClient {
  static final HttpClient _instance = HttpClient._internal();
  factory HttpClient() => _instance;
  HttpClient._internal();

  late Dio dio;
  final CookieJar cookieJar = CookieJar();

  void init() {
    dio = Dio();
    dio.interceptors.add(CookieManager(cookieJar));

    dio.options.baseUrl = 'http://10.0.2.2:8093';
    dio.options.connectTimeout = Duration(seconds: 10);
    dio.options.receiveTimeout = Duration(seconds: 10);
    dio.options.headers['Content-Type'] = 'application/x-www-form-urlencoded';

    dio.interceptors.add(InterceptorsWrapper(
      onRequest: (options, handler) {
        print('🔗 요청: ${options.method} ${options.uri}');
        handler.next(options);
      },
      onResponse: (response, handler) {
        print('✅ 응답: ${response.statusCode}');
        handler.next(response);
      },
      onError: (error, handler) {
        print('❌ 에러: ${error.message}');
        handler.next(error);
      },
    ));
  }

  Future<Response> get(String path) async {
    return await dio.get(path);
  }

  Future<Response> post(String path, {dynamic data}) async {
    return await dio.post(path, data: data);
  }
}

class CardInfo {
  final String cardno;
  final String cano;
  final String uid;
  final int cardcvc;
  final String cardname;
  final String cardstatus;
  final String carddate;

  CardInfo({
    required this.cardno,
    required this.cano,
    required this.uid,
    required this.cardcvc,
    required this.cardname,
    required this.cardstatus,
    required this.carddate,
  });

  factory CardInfo.fromJson(Map<String, dynamic> json) {
    return CardInfo(
      cardno: json['cardno'] ?? '4***-****-****-1234',
      cano: json['cano'] ?? '',
      uid: json['uid'] ?? '',
      cardcvc: json['cardcvc'] ?? 123,
      cardname: json['cardname'] ?? 'BNK 쇼핑환전체크카드',
      cardstatus: json['cardstatus'] ?? 'Y',
      carddate: json['carddate'] ?? '',
    );
  }
}

class CardManagementScreen extends StatefulWidget {
  @override
  _CardManagementScreenState createState() => _CardManagementScreenState();
}

class _CardManagementScreenState extends State<CardManagementScreen> {
  bool isCardActivated = true;
  String paymentCycle = '13일';
  bool isLoading = true;
  bool showingFullNumber = false;
  CardInfo? cardInfo;

  @override
  void initState() {
    super.initState();
    HttpClient().init();
    _loadCardInfo();
  }

  Future<void> _loadCardInfo() async {
    setState(() {
      isLoading = true;
    });

    try {
      print('카드 정보 요청 중...');
      Response response = await HttpClient().get('/user/card/info');

      print('카드 정보 응답 상태: ${response.statusCode}');
      print('카드 정보 응답 데이터: ${response.data}');

      if (response.statusCode == 200) {
        cardInfo = CardInfo.fromJson(response.data);
        isCardActivated = cardInfo!.cardstatus == 'Y';
        print('카드 정보 로딩 성공: ${cardInfo!.cardname}');
      } else if (response.statusCode == 401) {
        print('인증 실패 - 로그인 필요');
        _showMessage('로그인이 필요합니다.');
        _createDefaultCard();
      } else {
        print('카드 정보 로딩 실패 - 기본값 사용');
        _createDefaultCard();
      }
    } catch (e) {
      print('카드 정보 로딩 에러: $e');
      _createDefaultCard();
    }

    setState(() {
      isLoading = false;
    });
  }

  void _createDefaultCard() {
    cardInfo = CardInfo(
      cardno: '4***-****-****-1234',
      cano: '',
      uid: '',
      cardcvc: 123,
      cardname: 'BNK 쇼핑환전체크카드',
      cardstatus: 'Y',
      carddate: DateTime.now().toString(),
    );
    isCardActivated = true;
  }

  Future<void> _toggleCardStatus() async {
    try {
      print('카드 상태 변경 요청 중...');
      Response response = await HttpClient().post('/user/card/toggle-status');

      print('카드 상태 변경 응답: ${response.statusCode}');

      if (response.statusCode == 200) {
        setState(() {
          isCardActivated = !isCardActivated;
        });
        _showMessage('카드 상태가 변경되었습니다.');
      } else if (response.statusCode == 401) {
        _showMessage('로그인이 필요합니다.');
      } else {
        _showMessage('카드 상태 변경에 실패했습니다.');
      }
    } catch (e) {
      print('카드 상태 변경 에러: $e');
      _showMessage('카드 상태 변경 중 오류가 발생했습니다.');
    }
  }

  Future<void> _toggleCardNumber() async {
    if (!showingFullNumber) {
      try {
        print('전체 카드번호 요청 중...');
        Response response = await HttpClient().get('/user/card/full-number');

        print('전체 카드번호 응답: ${response.statusCode}');

        if (response.statusCode == 200) {
          setState(() {
            showingFullNumber = true;
          });
        } else if (response.statusCode == 401) {
          _showMessage('로그인이 필요합니다.');
        } else {
          setState(() {
            showingFullNumber = true;
          });
        }
      } catch (e) {
        print('카드번호 조회 에러: $e');
        setState(() {
          showingFullNumber = true;
        });
      }
    } else {
      setState(() {
        showingFullNumber = false;
      });
    }
  }

  void _showMessage(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: Color(0xFF1976D2),
        duration: Duration(seconds: 2),
      ),
    );
  }

  String _getDisplayCardNumber() {
    if (cardInfo == null) return '4***-****-****-1234';

    if (showingFullNumber) {
      String cleanNumber = cardInfo!.cardno.replaceAll('-', '').replaceAll('*', '');
      if (cleanNumber.length == 16) {
        return '${cleanNumber.substring(0, 4)}-${cleanNumber.substring(4, 8)}-${cleanNumber.substring(8, 12)}-${cleanNumber.substring(12, 16)}';
      }
      return '4000-1234-5678-1234';
    } else {
      return cardInfo!.cardno;
    }
  }

  String _getLastFourDigits() {
    if (cardInfo?.cardno != null) {
      String cleanNumber = cardInfo!.cardno.replaceAll('-', '').replaceAll('*', '');
      if (cleanNumber.length >= 4) {
        return cleanNumber.substring(cleanNumber.length - 4);
      }
    }
    return '1234';
  }

  String _getFirstDigit() {
    if (cardInfo?.cardno != null) {
      String cleanNumber = cardInfo!.cardno.replaceAll('-', '').replaceAll('*', '');
      if (cleanNumber.length >= 1) {
        return cleanNumber.substring(0, 1);
      }
    }
    return '4';
  }

  void _showCardBenefits() {}
  void _reportLostCard() {}
  void _cancelCard() {}
  void _changePaymentDate() {}

  @override
  Widget build(BuildContext context) {
    if (isLoading) {
      return Scaffold(
        backgroundColor: Colors.white,
        body: Center(
          child: CircularProgressIndicator(color: Color(0xFF1976D2)),
        ),
      );
    }

    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        backgroundColor: Colors.white,
        elevation: 1,
        shadowColor: Colors.grey.withValues(alpha: 0.3),
        leading: IconButton(
          icon: Icon(Icons.arrow_back, color: Color(0xFF1976D2)),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: Text(
          '뒤로가기',
          style: TextStyle(
            color: Color(0xFF1976D2),
            fontSize: 16,
            fontWeight: FontWeight.bold,
          ),
        ),
      ),
      body: RefreshIndicator(
        onRefresh: _loadCardInfo,
        child: SingleChildScrollView(
          physics: AlwaysScrollableScrollPhysics(),
          child: Padding(
            padding: EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '카드 관리',
                  style: TextStyle(
                    fontSize: 24,
                    fontWeight: FontWeight.bold,
                    color: Colors.black,
                  ),
                ),
                SizedBox(height: 20),
                Center(
                  child: Container(
                    width: 160,
                    height: 250,
                    margin: EdgeInsets.symmetric(vertical: 20),
                    decoration: BoxDecoration(
                      gradient: LinearGradient(
                        begin: Alignment.topLeft,
                        end: Alignment.bottomRight,
                        colors: [Color(0xFF1976D2), Color(0xFF42A5F5)],
                      ),
                      borderRadius: BorderRadius.circular(12),
                      boxShadow: [
                        BoxShadow(
                          color: Color(0xFF1976D2).withOpacity(0.3),
                          blurRadius: 8,
                          offset: Offset(0, 4),
                        ),
                      ],
                    ),
                    child: Padding(
                      padding: EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Container(
                            width: 30,
                            height: 20,
                            decoration: BoxDecoration(
                              color: Colors.amber,
                              borderRadius: BorderRadius.circular(4),
                            ),
                          ),
                          Spacer(),
                          Text(
                            '${_getFirstDigit()}*** **** **** ${_getLastFourDigits()}',
                            style: TextStyle(
                              color: Colors.white,
                              fontSize: 12,
                              fontFamily: 'monospace',
                              letterSpacing: 1.5,
                            ),
                          ),
                          SizedBox(height: 8),
                          Text(
                            cardInfo?.cardname ?? 'BNK 쇼핑환전체크카드',
                            style: TextStyle(
                              color: Colors.white,
                              fontSize: 10,
                              fontWeight: FontWeight.w500,
                            ),
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
                Center(
                  child: Column(
                    children: [
                      Text(
                        cardInfo?.cardname ?? 'BNK 쇼핑환전체크카드',
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.w600,
                          color: Colors.black87,
                        ),
                      ),
                      SizedBox(height: 4),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Container(
                            padding: EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                            decoration: BoxDecoration(
                              color: Colors.blue,
                              borderRadius: BorderRadius.circular(4),
                            ),
                            child: Text(
                              'VISA',
                              style: TextStyle(
                                color: Colors.white,
                                fontSize: 10,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ),
                          SizedBox(width: 6),
                          Text(
                            '(${_getLastFourDigits()})',
                            style: TextStyle(
                              fontSize: 14,
                              color: Colors.grey[600],
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
                SizedBox(height: 30),
                Container(
                  width: double.infinity,
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: Colors.grey[300]!),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Container(
                        padding: EdgeInsets.all(16),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              '카드번호',
                              style: TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.w600,
                                color: Colors.black87,
                              ),
                            ),
                            SizedBox(height: 12),
                            Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                Expanded(
                                  child: Text(
                                    _getDisplayCardNumber(),
                                    style: TextStyle(
                                      fontSize: 16,
                                      fontWeight: FontWeight.w600,
                                      color: Colors.black87,
                                      fontFamily: 'monospace',
                                    ),
                                  ),
                                ),
                                GestureDetector(
                                  onTap: _toggleCardNumber,
                                  child: Container(
                                    padding: EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                                    decoration: BoxDecoration(
                                      color: Colors.grey[200],
                                      borderRadius: BorderRadius.circular(16),
                                    ),
                                    child: Text(
                                      showingFullNumber ? '카드번호 숨기기' : '카드번호 보기',
                                      style: TextStyle(
                                        fontSize: 12,
                                        color: Colors.black87,
                                      ),
                                    ),
                                  ),
                                ),
                              ],
                            ),
                            SizedBox(height: 12),
                            Row(
                              children: [
                                Container(
                                  width: 40,
                                  height: 20,
                                  decoration: BoxDecoration(
                                    color: Colors.blue,
                                    borderRadius: BorderRadius.circular(4),
                                  ),
                                  child: Center(
                                    child: Text(
                                      'VISA',
                                      style: TextStyle(
                                        color: Colors.white,
                                        fontSize: 10,
                                        fontWeight: FontWeight.bold,
                                      ),
                                    ),
                                  ),
                                ),
                                SizedBox(width: 8),
                                Text(
                                  '국내외겸용',
                                  style: TextStyle(
                                    fontSize: 12,
                                    color: Colors.grey[600],
                                  ),
                                ),
                              ],
                            ),
                            SizedBox(height: 8),
                            Text(
                              '개인 정보 보호를 위해 CVC번호는 실물 카드에서만 확인 가능합니다.',
                              style: TextStyle(
                                fontSize: 11,
                                color: Colors.grey[600],
                                height: 1.3,
                              ),
                            ),
                          ],
                        ),
                      ),
                      Divider(height: 1, color: Colors.grey[300]),
                      Container(
                        padding: EdgeInsets.symmetric(horizontal: 16, vertical: 16),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text(
                              '카드 활성화',
                              style: TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.w600,
                                color: Colors.black87,
                              ),
                            ),
                            Switch(
                              value: isCardActivated,
                              onChanged: (value) => _toggleCardStatus(),
                              activeColor: Colors.blue,
                            ),
                          ],
                        ),
                      ),
                      Divider(height: 1, color: Colors.grey[300]),
                      GestureDetector(
                        onTap: _changePaymentDate,
                        child: Container(
                          padding: EdgeInsets.symmetric(horizontal: 16, vertical: 16),
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Text(
                                '결제일 변경',
                                style: TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.w600,
                                  color: Colors.black87,
                                ),
                              ),
                              Row(
                                children: [
                                  Text(
                                    paymentCycle,
                                    style: TextStyle(
                                      fontSize: 16,
                                      fontWeight: FontWeight.w600,
                                      color: Colors.black87,
                                    ),
                                  ),
                                  SizedBox(width: 8),
                                  Icon(
                                    Icons.arrow_forward_ios,
                                    size: 16,
                                    color: Colors.grey[600],
                                  ),
                                ],
                              ),
                            ],
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                SizedBox(height: 20),
                Container(
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: Colors.grey[300]!),
                  ),
                  child: Column(
                    children: [
                      _buildMenuTile('내 카드 혜택', onTap: _showCardBenefits),
                      Divider(height: 1, color: Colors.grey[200]),
                      _buildMenuTile('분실신고/해제', onTap: _reportLostCard),
                      Divider(height: 1, color: Colors.grey[200]),
                      _buildMenuTile('카드해지', onTap: _cancelCard),
                    ],
                  ),
                ),
                SizedBox(height: 40),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildMenuTile(String title, {VoidCallback? onTap}) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: EdgeInsets.symmetric(horizontal: 16, vertical: 16),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              title,
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w500,
                color: Colors.black87,
              ),
            ),
            Icon(
              Icons.arrow_forward_ios,
              size: 16,
              color: Colors.grey[600],
            ),
          ],
        ),
      ),
    );
  }
}