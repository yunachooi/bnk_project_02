import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import 'dart:async';
import 'package:url_launcher/url_launcher.dart';
import 'package:bnk_project_02f/account/accountMain.dart';
import 'package:kakao_flutter_sdk_common/kakao_flutter_sdk_common.dart'
    show KakaoSdk, launchBrowserTab;
import 'package:kakao_flutter_sdk_share/kakao_flutter_sdk_share.dart';
import 'package:kakao_flutter_sdk_template/kakao_flutter_sdk_template.dart';
import 'package:flutter/services.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  KakaoSdk.init(
    nativeAppKey: '',
    javaScriptAppKey: '',
  );
  runApp(const ShoppingApp());
}

class ShoppingApp extends StatelessWidget {
  const ShoppingApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '해외직구쇼핑몰',
      theme: ThemeData(
        primarySwatch: Colors.blue,
        primaryColor: const Color(0xFF1976D2),
      ),
      home: const ShoppingHomePage(),
      debugShowCheckedModeBanner: false,
    );
  }
}

class Product {
  final String spno;
  final String spname;
  final String? spnameKo;
  final String spdescription;
  final String? spdescriptionKo;
  final double spprice;
  final String spcurrency;
  final double sprating;
  final int spreviews;
  final String spimgurl;
  final String spurl;
  final String? spat;

  Product({
    required this.spno,
    required this.spname,
    this.spnameKo,
    required this.spdescription,
    this.spdescriptionKo,
    required this.spprice,
    required this.spcurrency,
    required this.sprating,
    required this.spreviews,
    required this.spimgurl,
    required this.spurl,
    this.spat,
  });

  factory Product.fromJson(Map<String, dynamic> json) {
    return Product(
      spno: json['spno'] ?? '',
      spname: json['spname'] ?? '',
      spnameKo: json['spnameKo'] ?? json['spname_ko'],
      spdescription: json['spdescription'] ?? '',
      spdescriptionKo: json['spdescriptionKo'] ?? json['spdescription_ko'],
      spprice: (json['spprice'] ?? 0).toDouble(),
      spcurrency: json['spcurrency'] ?? 'USD',
      sprating: (json['sprating'] ?? 0).toDouble(),
      spreviews: (json['spreviews'] ?? 0).toInt(),
      spimgurl: json['spimgurl'] ?? '',
      spurl: json['spurl'] ?? '',
      spat: json['spat'],
    );
  }

  String get displayName =>
      (spnameKo != null && spnameKo!.isNotEmpty) ? spnameKo! : spname;

  String get displayDescription =>
      (spdescriptionKo != null && spdescriptionKo!.isNotEmpty)
          ? spdescriptionKo!
          : spdescription;

  String get formattedReviews => _addCommas(spreviews);

  String _addCommas(int number) {
    return number.toString().replaceAllMapped(
      RegExp(r'(\d{1,3})(?=(\d{3})+(?!\d))'),
          (Match match) => '${match[1]},',
    );
  }
}

class ApiService {
  static const String baseUrl = 'http://10.0.2.2:8093';

  // 공유 링크(HMAC URL)
  static Future<String?> getShareUrl(String spno) async {
    try {
      final response = await http
          .get(
        Uri.parse('$baseUrl/api/share/product/$spno'),
        headers: {'Accept': 'text/plain'},
      )
          .timeout(const Duration(seconds: 10));

      if (response.statusCode == 200 && response.body.isNotEmpty) {
        return response.body;
      } else {
        return null;
      }
    } catch (e) {
      return null;
    }
  }

  static Future<List<Product>> getProducts() async {
    try {
      final response = await http
          .get(
        Uri.parse('$baseUrl/user/shopping/products'),
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
      )
          .timeout(const Duration(seconds: 15));

      if (response.statusCode == 200) {
        final responseBody = response.body;
        if (responseBody.isEmpty) return [];

        List<dynamic> jsonList = json.decode(responseBody);
        List<Product> products = jsonList.map((json) {
          try {
            return Product.fromJson(json);
          } catch (_) {
            return null;
          }
        }).where((p) => p != null).cast<Product>().toList();

        return products;
      } else {
        throw Exception('서버 오류: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('네트워크 오류: $e');
    }
  }

  static Future<Map<String, dynamic>> getUserInfo() async {
    try {
      final response = await http
          .get(
        Uri.parse('$baseUrl/user/shopping/user/info'),
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
      )
          .timeout(const Duration(seconds: 10));

      if (response.statusCode == 200) {
        final responseBody = response.body;
        if (responseBody.isEmpty) {
          return {'isLoggedIn': false, 'uname': '회원'};
        }
        return json.decode(responseBody);
      } else {
        return {'isLoggedIn': false, 'uname': '회원'};
      }
    } catch (_) {
      return {'isLoggedIn': false, 'uname': '회원'};
    }
  }
}

class ProductDetailPage extends StatelessWidget {
  final Product product;

  const ProductDetailPage({
    super.key,
    required this.product,
  });

  Future<void> _launchURL() async {
    final Uri url = Uri.parse(product.spurl);
    if (!await launchUrl(url, mode: LaunchMode.externalApplication)) {
      throw Exception('링크를 열 수 없습니다: ${product.spurl}');
    }
  }

  // 상세화면에서 현재 상품 공유
  Future<void> _shareProduct(BuildContext context) async {
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        duration: Duration(milliseconds: 700),
        content: Text('공유 준비중...'),
      ),
    );

    String shareUrlForClipboard = product.spurl;
    try {
      // 1) 서버 서명 URL (타임아웃 시 폴백)
      final raw = await ApiService.getShareUrl(product.spno)
          .timeout(const Duration(seconds: 2), onTimeout: () => null);

      String shareUrl = (raw != null && raw.isNotEmpty) ? raw : product.spurl;
      shareUrlForClipboard = shareUrl;

      // 2) https 강제
      final u = Uri.tryParse(shareUrl);
      if (u == null || u.scheme != 'https') {
        shareUrl = 'https://www.busanbank.co.kr'; // 안전 https 폴백
      }

      // 3) 이미지 https 보장
      final img = product.spimgurl.startsWith('http')
          ? product.spimgurl
          : 'https://via.placeholder.com/600x400.png?text=BNK+SHOP';

      final template = FeedTemplate(
        content: Content(
          title: product.displayName,
          description: product.displayDescription,
          imageUrl: Uri.parse(img),
          link: Link(
            webUrl: Uri.parse(shareUrl),
            mobileWebUrl: Uri.parse(shareUrl),
          ),
        ),
        buttons: [
          Button(
            title: '자세히 보기',
            link: Link(
              webUrl: Uri.parse(shareUrl),
              mobileWebUrl: Uri.parse(shareUrl),
            ),
          ),
        ],
      );

      // 4) 카톡 설치 여부
      final installed = await ShareClient.instance.isKakaoTalkSharingAvailable();
      if (installed) {
        final uri = await ShareClient.instance.shareDefault(template: template);
        await ShareClient.instance.launchKakaoTalk(uri);
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('카카오톡 공유창을 열었어요 ✅')),
          );
        }
        return;
      }

      // 5) 미설치 시 웹 공유 (3단 폴백)
      final sharerUrl = await WebSharerClient.instance.makeDefaultUrl(template: template);

      try {
        await launchBrowserTab(sharerUrl, popupOpen: true);
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('웹 공유창 열렸어요(브라우저 탭) ✅')),
          );
        }
        return;
      } catch (_) {}

      final okExternal =
      await launchUrl(sharerUrl, mode: LaunchMode.externalApplication);
      if (okExternal) {
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('웹 공유창 열렸어요(외부 브라우저) ✅')),
          );
        }
        return;
      }

      final okInApp = await launchUrl(sharerUrl, mode: LaunchMode.inAppWebView);
      if (okInApp) {
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('웹 공유창 열렸어요(인앱 웹뷰) ✅')),
          );
        }
        return;
      }

      // 6) 모두 실패 시 링크 복사
      await Clipboard.setData(ClipboardData(text: shareUrlForClipboard));
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('브라우저가 없어 링크 복사함 📋: $shareUrlForClipboard')),
        );
      }
    } catch (e, st) {
      debugPrint('[share][detail][error] $e\n$st');
      await Clipboard.setData(ClipboardData(text: shareUrlForClipboard));
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('공유 실패. 링크 복사함 📋: $shareUrlForClipboard')),
        );
      }
    }
  }

  void _showQuickShareToast(BuildContext context,
      {String text = '공유 준비중...'}) {
    final entry = OverlayEntry(
      builder: (_) => Positioned.fill(
        child: IgnorePointer(
          ignoring: true,
          child: Center(
            child: Material(
              color: Colors.transparent,
              child: Container(
                padding:
                const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                decoration: BoxDecoration(
                  color: Colors.black.withOpacity(0.75),
                  borderRadius: BorderRadius.circular(999),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: const [
                    SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(
                          strokeWidth: 2, color: Colors.white),
                    ),
                    SizedBox(width: 10),
                    Text('공유 준비중...', style: TextStyle(color: Colors.white)),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
    Overlay.of(context, rootOverlay: true)?.insert(entry);
    Future.delayed(const Duration(seconds: 1), () => entry.remove());
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        backgroundColor: Colors.white,
        elevation: 1,
        shadowColor: Colors.grey.withOpacity(0.3),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: Color(0xFF1976D2)),
          onPressed: () {
            Navigator.of(context).pop();
          },
        ),
        title: const Text(
          '뒤로가기',
          style: TextStyle(
            color: Color(0xFF1976D2),
            fontSize: 16,
            fontWeight: FontWeight.bold,
          ),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.shopping_cart, color: Colors.black),
            onPressed: () {},
          ),
          IconButton(
            icon: const Icon(Icons.share, color: Colors.black),
            onPressed: () {
              HapticFeedback.selectionClick();
              _showQuickShareToast(context);
              _shareProduct(context);
            },
          ),
        ],
      ),
      body: SingleChildScrollView(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                '해외직구쇼핑몰',
                style: TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.bold,
                  color: Colors.black,
                ),
              ),
              const SizedBox(height: 20),
              Container(
                width: double.infinity,
                height: 300,
                decoration: BoxDecoration(
                  color: Colors.grey[200],
                  borderRadius: BorderRadius.circular(8),
                ),
                child: product.spimgurl.isEmpty
                    ? Center(
                  child: Icon(
                    Icons.image,
                    color: Colors.grey[400],
                    size: 60,
                  ),
                )
                    : ClipRRect(
                  borderRadius: BorderRadius.circular(8),
                  child: Image.network(
                    product.spimgurl,
                    fit: BoxFit.cover,
                    loadingBuilder: (context, child, progress) {
                      if (progress == null) return child;
                      return Center(
                        child: CircularProgressIndicator(
                          value: progress.expectedTotalBytes != null
                              ? progress.cumulativeBytesLoaded /
                              progress.expectedTotalBytes!
                              : null,
                        ),
                      );
                    },
                    errorBuilder: (_, __, ___) => Center(
                      child: Icon(
                        Icons.broken_image,
                        color: Colors.grey[400],
                        size: 60,
                      ),
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 16),
              Row(
                children: [
                  ...List.generate(5, (index) {
                    return Icon(
                      Icons.star,
                      size: 18,
                      color: index < product.sprating.floor()
                          ? Colors.orange
                          : Colors.grey[300],
                    );
                  }),
                  const SizedBox(width: 8),
                  Text(
                    '(${product.formattedReviews}개 리뷰)',
                    style: TextStyle(
                      fontSize: 14,
                      color: Colors.grey[600],
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Text(
                product.displayName,
                style: const TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: Colors.black,
                ),
              ),
              const SizedBox(height: 20),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: const Color(0xFFE3F2FD),
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: const Color(0xFF90CAF9), width: 1),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      '가격 정보',
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                        color: Color(0xFF0D47A1),
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      '${product.spprice.toStringAsFixed(2)} ${product.spcurrency}',
                      style: const TextStyle(
                        fontSize: 16,
                        color: Colors.black,
                        decoration: TextDecoration.lineThrough,
                        decorationColor: Colors.black,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      '${(product.spprice * 0.9).toStringAsFixed(2)} ${product.spcurrency} (카드 할인 적용 시)',
                      style: const TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.bold,
                        color: Color(0xFF1976D2),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 20),
              GestureDetector(
                onTap: () async {
                  try {
                    final Uri url =
                    Uri.parse('https://www.busanbank.co.kr');
                    if (!await launchUrl(url,
                        mode: LaunchMode.externalApplication)) {
                      throw Exception('Could not launch $url');
                    }
                  } catch (e) {
                    if (context.mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(
                          content:
                          Text('링크를 열 수 없습니다: ${e.toString()}'),
                          backgroundColor: Colors.red,
                          duration: const Duration(seconds: 3),
                        ),
                      );
                    }
                  }
                },
                child: SizedBox(
                  width: double.infinity,
                  height: 150,
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(8),
                    child: Image.asset(
                      'assets/images/ad4.jpg',
                      fit: BoxFit.cover,
                      errorBuilder: (_, __, ___) => Container(
                        padding: const EdgeInsets.all(16),
                        decoration: BoxDecoration(
                          color: Colors.grey[200],
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: const Center(
                          child: Text(
                            '추천상품상품광고',
                            style: TextStyle(
                              fontSize: 16,
                              fontWeight: FontWeight.w500,
                              color: Colors.black,
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 20),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: Colors.orange[50],
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: Colors.orange[300]!, width: 2),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: const [
                    Row(
                      children: [
                        Icon(
                          Icons.warning_amber_rounded,
                          color: Color(0xFFFFA726),
                          size: 24,
                        ),
                        SizedBox(width: 8),
                        Text(
                          '해외 직구 주의사항',
                          style: TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                            color: Color(0xFFEF6C00),
                          ),
                        ),
                      ],
                    ),
                    SizedBox(height: 12),
                    Text(
                      '• 아마존 글로벌 스토어에서 판매 중인 상품으로\n'
                          '공식 판매자인 아마존 미국에서 판매/배송을 책임집니다.\n'
                          '• 제품 문의 시 아마존 고객센터로 문의해주세요',
                      style: TextStyle(
                        fontSize: 14,
                        color: Color(0xFFF57C00),
                        height: 1.4,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 20),
              const Text(
                '상품상세설명',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  color: Colors.black,
                ),
              ),
              const SizedBox(height: 12),
              Text(
                product.displayDescription,
                style: const TextStyle(
                  fontSize: 18,
                  color: Colors.black87,
                  height: 1.5,
                ),
              ),
              const SizedBox(height: 30),
              SizedBox(
                width: double.infinity,
                height: 50,
                child: ElevatedButton(
                  onPressed: () async {
                    try {
                      await _launchURL();
                    } catch (e) {
                      if (context.mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(
                            content:
                            Text('링크를 열 수 없습니다: ${e.toString()}'),
                            backgroundColor: Colors.red,
                            duration: const Duration(seconds: 3),
                          ),
                        );
                      }
                    }
                  },
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF1976D2),
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(8),
                    ),
                  ),
                  child: const Text(
                    '판매 사이트에서 구매하기',
                    style: TextStyle(
                        fontSize: 16, fontWeight: FontWeight.bold),
                  ),
                ),
              ),
              const SizedBox(height: 20),
            ],
          ),
        ),
      ),
    );
  }
}

class ShoppingHomePage extends StatefulWidget {
  const ShoppingHomePage({super.key});

  @override
  State<ShoppingHomePage> createState() => _ShoppingHomePageState();
}

class _ShoppingHomePageState extends State<ShoppingHomePage> {
  final TextEditingController _searchController = TextEditingController();
  final PageController _pageController = PageController();
  String selectedCategory = '전체';
  List<String> categories = ['전체', 'USD', 'KRW', 'JPY', 'CNY'];
  List<String> categoryLabels = ['추천 상품', 'USD', 'KRW', 'JPY', 'CNY'];
  List<String> categoryImages = [
    '',
    'assets/images/usd.png',
    'assets/images/kor.png',
    'assets/images/jpy.png',
    'assets/images/cnh.png'
  ];
  List<String> adImages = [
    'assets/images/ad1.png',
    'assets/images/ad2.png',
    'assets/images/ad3.png'
  ];

  List<Product> products = [];
  List<Product> filteredProducts = [];
  bool isLoading = true;
  String errorMessage = '';
  int currentAdIndex = 0;
  String username = '회원';
  bool isLoggedIn = false;

  @override
  void initState() {
    super.initState();
    loadProducts();
    loadUserInfo();
    _startAdSlider();
  }

  @override
  void dispose() {
    _pageController.dispose();
    _searchController.dispose();
    super.dispose();
  }

  void _startAdSlider() {
    Timer.periodic(const Duration(seconds: 3), (Timer timer) {
      if (!mounted) {
        timer.cancel();
        return;
      }
      if (_pageController.hasClients) {
        int nextPage = _pageController.page!.round() + 1;
        if (nextPage >= adImages.length) nextPage = 0;
        _pageController.animateToPage(
          nextPage,
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeIn,
        );
      }
    });
  }

  Future<void> loadUserInfo() async {
    try {
      final userInfo = await ApiService.getUserInfo();
      setState(() {
        isLoggedIn = userInfo['isLoggedIn'] ?? false;
        username = userInfo['uname'] ?? '회원';
      });
    } catch (_) {
      setState(() {
        isLoggedIn = false;
        username = '회원';
      });
    }
  }

  Future<void> loadProducts() async {
    try {
      setState(() {
        isLoading = true;
        errorMessage = '';
      });

      final productList = await ApiService.getProducts();

      setState(() {
        products = productList;
        filteredProducts = filterByCategory(productList);
        isLoading = false;
      });
    } catch (e) {
      setState(() {
        isLoading = false;
        errorMessage = e.toString();
      });
    }
  }

  void filterProducts(String query) {
    setState(() {
      if (query.isEmpty) {
        filteredProducts = filterByCategory(products);
      } else {
        List<Product> searchResults = products
            .where((p) =>
        p.displayName.toLowerCase().contains(query.toLowerCase()) ||
            p.displayDescription
                .toLowerCase()
                .contains(query.toLowerCase()) ||
            p.spname.toLowerCase().contains(query.toLowerCase()) ||
            p.spdescription.toLowerCase().contains(query.toLowerCase()))
            .toList();
        filteredProducts = filterByCategory(searchResults);
      }
    });
  }

  List<Product> filterByCategory(List<Product> productList) {
    if (selectedCategory == '전체') {
      return productList.take(4).toList();
    }
    final currency = selectedCategory;
    return productList
        .where((p) => p.spcurrency.toUpperCase() == currency)
        .toList();
  }

  void selectCategory(String category) {
    setState(() {
      selectedCategory = category;
      filteredProducts = filterByCategory(products);
      _searchController.clear();
    });
  }

  // ✅ 홈 화면에서 첫 상품 공유 (공유 버튼용)
  Future<void> _shareFromHome(BuildContext context) async {
    if (filteredProducts.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('공유할 상품이 없습니다. 먼저 상품을 불러오세요.')),
      );
      return;
    }

    final p = filteredProducts.first;

    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        duration: Duration(milliseconds: 700),
        content: Text('공유 준비중...'),
      ),
    );

    String shareUrlForClipboard = p.spurl;
    try {
      final raw = await ApiService.getShareUrl(p.spno)
          .timeout(const Duration(seconds: 2), onTimeout: () => null);

      String shareUrl = (raw != null && raw.isNotEmpty) ? raw : p.spurl;
      shareUrlForClipboard = shareUrl;

      final u = Uri.tryParse(shareUrl);
      if (u == null || u.scheme != 'https') {
        shareUrl = 'https://www.busanbank.co.kr';
      }

      final img = p.spimgurl.startsWith('http')
          ? p.spimgurl
          : 'https://via.placeholder.com/600x400.png?text=BNK+SHOP';

      final template = FeedTemplate(
        content: Content(
          title: p.displayName,
          description: p.displayDescription,
          imageUrl: Uri.parse(img),
          link: Link(
            webUrl: Uri.parse(shareUrl),
            mobileWebUrl: Uri.parse(shareUrl),
          ),
        ),
        buttons: [
          Button(
            title: '자세히 보기',
            link:
            Link(webUrl: Uri.parse(shareUrl), mobileWebUrl: Uri.parse(shareUrl)),
          ),
        ],
      );

      final installed = await ShareClient.instance.isKakaoTalkSharingAvailable();
      if (installed) {
        final uri = await ShareClient.instance.shareDefault(template: template);
        await ShareClient.instance.launchKakaoTalk(uri);
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('카카오톡 공유창을 열었어요 ✅')),
          );
        }
        return;
      }

      final sharerUrl = await WebSharerClient.instance.makeDefaultUrl(template: template);

      try {
        await launchBrowserTab(sharerUrl, popupOpen: true);
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('웹 공유창 열렸어요(브라우저 탭) ✅')),
          );
        }
        return;
      } catch (_) {}

      final okExternal =
      await launchUrl(sharerUrl, mode: LaunchMode.externalApplication);
      if (okExternal) {
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('웹 공유창 열렸어요(외부 브라우저) ✅')),
          );
        }
        return;
      }

      final okInApp = await launchUrl(sharerUrl, mode: LaunchMode.inAppWebView);
      if (okInApp) {
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('웹 공유창 열렸어요(인앱 웹뷰) ✅')),
          );
        }
        return;
      }

      await Clipboard.setData(ClipboardData(text: shareUrlForClipboard));
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('브라우저가 없어 링크 복사함 📋: $shareUrlForClipboard')),
        );
      }
    } catch (e, st) {
      debugPrint('[share][home][error] $e\n$st');
      await Clipboard.setData(ClipboardData(text: shareUrlForClipboard));
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('공유 실패. 링크 복사함 📋: $shareUrlForClipboard')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        backgroundColor: Colors.white,
        elevation: 1,
        shadowColor: Colors.grey.withOpacity(0.3),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: Colors.black),
          onPressed: () {
            Navigator.pushReplacement(
              context,
              MaterialPageRoute(
                builder: (context) => const AccountMainPage(),
              ),
            );
          },
        ),
        title: const Text(
          '뒤로가기',
          style: TextStyle(
            color: Color(0xFF1976D2),
            fontSize: 16,
            fontWeight: FontWeight.bold,
          ),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.shopping_cart, color: Colors.black),
            onPressed: () {},
          ),
          IconButton(
            icon: const Icon(Icons.share, color: Colors.black),
            onPressed: () {
              HapticFeedback.selectionClick();
              _shareFromHome(context); // ✅ 실제 공유 수행
            },
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: loadProducts,
        child: SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  '해외직구쇼핑몰',
                  style: TextStyle(
                    fontSize: 24,
                    fontWeight: FontWeight.bold,
                    color: Colors.black,
                  ),
                ),
                const SizedBox(height: 16),
                Container(
                  width: double.infinity,
                  height: 250,
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: const Color(0xFF1976D2), width: 2),
                  ),
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(6),
                    child: PageView.builder(
                      controller: _pageController,
                      onPageChanged: (index) {
                        setState(() {
                          currentAdIndex = index;
                        });
                      },
                      itemCount: adImages.length,
                      itemBuilder: (context, index) {
                        return Image.asset(
                          adImages[index],
                          fit: BoxFit.cover,
                          errorBuilder: (_, __, ___) => Container(
                            color: Colors.grey[200],
                            child: Center(
                              child: Column(
                                mainAxisAlignment: MainAxisAlignment.center,
                                children: [
                                  Icon(
                                    Icons.image,
                                    color: Colors.grey[400],
                                    size: 32,
                                  ),
                                  const SizedBox(height: 8),
                                  Text(
                                    '광고 ${index + 1}',
                                    style: const TextStyle(
                                      fontSize: 16,
                                      color: Colors.black,
                                      fontWeight: FontWeight.w500,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                        );
                      },
                    ),
                  ),
                ),
                const SizedBox(height: 20),
                Container(
                  padding:
                  const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                  child: Row(
                    children: [
                      Icon(Icons.search, color: Colors.grey[600], size: 20),
                      const SizedBox(width: 12),
                      Expanded(
                        child: TextField(
                          controller: _searchController,
                          onChanged: filterProducts,
                          decoration: InputDecoration(
                            hintText: '상품을 검색하세요',
                            border: InputBorder.none,
                            hintStyle: TextStyle(
                              color: Colors.grey[600],
                              fontSize: 14,
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 20),
                SingleChildScrollView(
                  scrollDirection: Axis.horizontal,
                  child: Row(
                    children: List.generate(categories.length, (index) {
                      String category = categories[index];
                      String label = categoryLabels[index];
                      String imagePath = categoryImages[index];
                      bool isSelected = selectedCategory == category;

                      return GestureDetector(
                        onTap: () => selectCategory(category),
                        child: Container(
                          margin: const EdgeInsets.only(right: 16),
                          child: Column(
                            children: [
                              Container(
                                width: 60,
                                height: 60,
                                decoration: BoxDecoration(
                                  shape: BoxShape.circle,
                                  color: isSelected
                                      ? const Color(0xFF1976D2)
                                      : Colors.grey[200],
                                  border: isSelected
                                      ? Border.all(
                                    color: const Color(0xFF1976D2),
                                    width: 3,
                                  )
                                      : null,
                                ),
                                child: Center(
                                  child: category == '전체'
                                      ? Text(
                                    '전체',
                                    style: TextStyle(
                                      color: isSelected
                                          ? Colors.white
                                          : Colors.black,
                                      fontSize: 12,
                                      fontWeight: FontWeight.bold,
                                    ),
                                  )
                                      : SizedBox(
                                    width: 50,
                                    height: 50,
                                    child: ClipOval(
                                      child: Image.asset(
                                        imagePath,
                                        fit: BoxFit.cover,
                                        errorBuilder: (_, __, ___) =>
                                            Container(
                                              decoration: BoxDecoration(
                                                shape: BoxShape.circle,
                                                color: Colors.grey[300],
                                              ),
                                              child: Center(
                                                child: Text(
                                                  category,
                                                  style: const TextStyle(
                                                    color: Colors.black,
                                                    fontSize: 10,
                                                    fontWeight:
                                                    FontWeight.bold,
                                                  ),
                                                ),
                                              ),
                                            ),
                                      ),
                                    ),
                                  ),
                                ),
                              ),
                              const SizedBox(height: 8),
                              Text(
                                label,
                                style: TextStyle(
                                  fontSize: 12,
                                  color: isSelected
                                      ? const Color(0xFF1976D2)
                                      : Colors.black,
                                  fontWeight: isSelected
                                      ? FontWeight.bold
                                      : FontWeight.normal,
                                ),
                              ),
                            ],
                          ),
                        ),
                      );
                    }),
                  ),
                ),
                const SizedBox(height: 20),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      selectedCategory == '전체'
                          ? '$username님을 위한 추천 상품'
                          : '상품 목록 (${filteredProducts.length}개)',
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                        color: Colors.black,
                      ),
                    ),
                    if (isLoading)
                      const SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      ),
                  ],
                ),
                const SizedBox(height: 16),
                if (errorMessage.isNotEmpty)
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: const Color(0xFFE3F2FD),
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: const Color(0xFF90CAF9)),
                    ),
                    child: Column(
                      children: [
                        const Text(
                          '데이터를 불러오는 중 오류가 발생했습니다',
                          style: TextStyle(
                              color: Color(0xFF0D47A1),
                              fontWeight: FontWeight.bold),
                        ),
                        const SizedBox(height: 8),
                        Text(
                          errorMessage,
                          style: const TextStyle(
                              color: Color(0xFF1976D2), fontSize: 12),
                        ),
                        const SizedBox(height: 12),
                        ElevatedButton(
                          onPressed: loadProducts,
                          style: ElevatedButton.styleFrom(
                            backgroundColor: const Color(0xFF1976D2),
                            foregroundColor: Colors.white,
                          ),
                          child: const Text('다시 시도'),
                        ),
                      ],
                    ),
                  ),
                if (!isLoading && errorMessage.isEmpty)
                  filteredProducts.isEmpty
                      ? SizedBox(
                    height: 200,
                    child: Center(
                      child: Text(
                        '검색 결과가 없습니다',
                        style: TextStyle(
                          fontSize: 16,
                          color: Colors.grey[600],
                        ),
                      ),
                    ),
                  )
                      : GridView.builder(
                    shrinkWrap: true,
                    physics: const NeverScrollableScrollPhysics(),
                    gridDelegate:
                    const SliverGridDelegateWithFixedCrossAxisCount(
                      crossAxisCount: 2,
                      crossAxisSpacing: 16,
                      mainAxisSpacing: 20,
                      childAspectRatio: 0.55,
                    ),
                    itemCount: filteredProducts.length,
                    itemBuilder: (context, index) {
                      return GestureDetector(
                        onTap: () {
                          Navigator.push(
                            context,
                            MaterialPageRoute(
                              builder: (context) => ProductDetailPage(
                                  product: filteredProducts[index]),
                            ),
                          );
                        },
                        child: ProductCard(
                            product: filteredProducts[index]),
                      );
                    },
                  ),
                if (isLoading && errorMessage.isEmpty)
                  const SizedBox(
                    height: 200,
                    child: Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          CircularProgressIndicator(),
                          SizedBox(height: 16),
                          Text(
                            '상품을 불러오는 중...',
                            style: TextStyle(
                              fontSize: 16,
                              color: Colors.grey,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                const SizedBox(height: 20),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class ProductCard extends StatelessWidget {
  final Product product;

  const ProductCard({
    super.key,
    required this.product,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(8),
        boxShadow: [
          BoxShadow(
            color: Colors.grey.withOpacity(0.1),
            spreadRadius: 1,
            blurRadius: 3,
            offset: const Offset(0, 1),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            flex: 4,
            child: Container(
              width: double.infinity,
              decoration: BoxDecoration(
                color: Colors.grey[200],
                borderRadius:
                const BorderRadius.vertical(top: Radius.circular(8)),
              ),
              child: product.spimgurl.isEmpty
                  ? Center(
                child: Icon(
                  Icons.image,
                  color: Colors.grey[400],
                  size: 40,
                ),
              )
                  : ClipRRect(
                borderRadius: const BorderRadius.vertical(
                    top: Radius.circular(8)),
                child: Image.network(
                  product.spimgurl,
                  fit: BoxFit.cover,
                  loadingBuilder: (context, child, progress) {
                    if (progress == null) return child;
                    return Center(
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        value: progress.expectedTotalBytes != null
                            ? progress.cumulativeBytesLoaded /
                            progress.expectedTotalBytes!
                            : null,
                      ),
                    );
                  },
                  errorBuilder: (_, __, ___) => Center(
                    child: Icon(
                      Icons.broken_image,
                      color: Colors.grey[400],
                      size: 40,
                    ),
                  ),
                ),
              ),
            ),
          ),
          Expanded(
            flex: 3,
            child: Padding(
              padding: const EdgeInsets.all(6),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Row(
                    children: [
                      ...List.generate(5, (index) {
                        return Icon(
                          Icons.star,
                          size: 14,
                          color: index < product.sprating.floor()
                              ? Colors.orange
                              : Colors.grey[300],
                        );
                      }),
                      const SizedBox(width: 4),
                      Text(
                        '(${product.formattedReviews})',
                        style: TextStyle(
                          fontSize: 11,
                          color: Colors.grey[600],
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 4),
                  Text(
                    product.displayName,
                    style: const TextStyle(
                      fontSize: 13,
                      fontWeight: FontWeight.bold,
                      color: Colors.black,
                    ),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                  const SizedBox(height: 4),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '${product.spprice.toStringAsFixed(2)} ${product.spcurrency}',
                        style: const TextStyle(
                          fontSize: 12,
                          color: Colors.black,
                          decoration: TextDecoration.lineThrough,
                          decorationColor: Colors.black,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        '${(product.spprice * 0.9).toStringAsFixed(2)} ${product.spcurrency}',
                        style: const TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.bold,
                          color: Color(0xFF1976D2),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}