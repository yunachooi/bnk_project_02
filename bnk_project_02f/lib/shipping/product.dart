import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';

void main() {
  runApp(ShoppingApp());
}

class ShoppingApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '해외직구쇼핑몰',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: ShoppingHomePage(),
      debugShowCheckedModeBanner: false,
    );
  }
}

class Product {
  final String spno;
  final String spname;
  final String spdescription;
  final double spprice;
  final String spcurrency;
  final double sprating;
  final int spreviews;
  final String spurl;
  final String? spat;

  Product({
    required this.spno,
    required this.spname,
    required this.spdescription,
    required this.spprice,
    required this.spcurrency,
    required this.sprating,
    required this.spreviews,
    required this.spurl,
    this.spat,
  });

  factory Product.fromJson(Map<String, dynamic> json) {
    return Product(
      spno: json['spno'] ?? '',
      spname: json['spname'] ?? '',
      spdescription: json['spdescription'] ?? '',
      spprice: (json['spprice'] ?? 0).toDouble(),
      spcurrency: json['spcurrency'] ?? 'USD',
      sprating: (json['sprating'] ?? 0).toDouble(),
      spreviews: json['spreviews'] ?? 0,
      spurl: json['spurl'] ?? '',
      spat: json['spat'],
    );
  }
}

class ApiService {
  static const String baseUrl = 'http://10.0.2.2:8093';

  static Future<List<Product>> getProducts() async {
    try {
      print('API 호출 시작: $baseUrl/shopping/products');

      final response = await http.get(
        Uri.parse('$baseUrl/shopping/products'),
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
      ).timeout(Duration(seconds: 15));

      print('API 응답 상태: ${response.statusCode}');

      if (response.statusCode == 200) {
        final responseBody = response.body;
        print('응답 길이: ${responseBody.length}');

        if (responseBody.isEmpty) {
          print('응답 본문이 비어있음');
          return [];
        }

        List<dynamic> jsonList = json.decode(responseBody);
        print('파싱된 상품 개수: ${jsonList.length}');

        List<Product> products = jsonList.map((json) {
          try {
            return Product.fromJson(json);
          } catch (e) {
            print('상품 파싱 오류: $e');
            return null;
          }
        }).where((product) => product != null).cast<Product>().toList();

        print('성공적으로 파싱된 상품: ${products.length}개');
        return products;

      } else {
        throw Exception('서버 오류: ${response.statusCode}');
      }
    } catch (e) {
      print('API 호출 오류: $e');
      throw Exception('네트워크 오류: $e');
    }
  }
}

class ShoppingHomePage extends StatefulWidget {
  @override
  _ShoppingHomePageState createState() => _ShoppingHomePageState();
}

class _ShoppingHomePageState extends State<ShoppingHomePage> {
  final TextEditingController _searchController = TextEditingController();
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

  List<Product> products = [];
  List<Product> filteredProducts = [];
  bool isLoading = true;
  String errorMessage = '';

  @override
  void initState() {
    super.initState();
    loadProducts();
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
            .where((product) =>
        product.spname.toLowerCase().contains(query.toLowerCase()) ||
            product.spdescription.toLowerCase().contains(query.toLowerCase()))
            .toList();
        filteredProducts = filterByCategory(searchResults);
      }
    });
  }

  List<Product> filterByCategory(List<Product> productList) {
    if (selectedCategory == '전체') {
      return productList;
    }

    String currency = selectedCategory;
    return productList.where((product) =>
    product.spcurrency.toUpperCase() == currency).toList();
  }

  void selectCategory(String category) {
    setState(() {
      selectedCategory = category;
      filteredProducts = filterByCategory(products);
      _searchController.clear();
    });
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
          icon: Icon(Icons.arrow_back, color: Colors.black),
          onPressed: () {
            Navigator.of(context).pop();
          },
        ),
        title: Text(
          '뒤로가기',
          style: TextStyle(
            color: Colors.blue,
            fontSize: 16,
            fontWeight: FontWeight.bold,
          ),
        ),
        actions: [
          IconButton(
            icon: Icon(Icons.shopping_cart, color: Colors.black),
            onPressed: () {},
          ),
          IconButton(
            icon: Icon(Icons.share, color: Colors.black),
            onPressed: () {},
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: loadProducts,
        child: SingleChildScrollView(
          physics: AlwaysScrollableScrollPhysics(),
          child: Padding(
            padding: EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '해외직구쇼핑몰',
                  style: TextStyle(
                    fontSize: 24,
                    fontWeight: FontWeight.bold,
                    color: Colors.black,
                  ),
                ),
                SizedBox(height: 16),

                Container(
                  width: double.infinity,
                  height: 120,
                  decoration: BoxDecoration(
                    color: Colors.grey[200],
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: Colors.blue, width: 2),
                  ),
                  child: Center(
                    child: Text(
                      '추천상품광고',
                      style: TextStyle(
                        fontSize: 16,
                        color: Colors.black,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ),
                ),
                SizedBox(height: 20),

                Container(
                  padding: EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                  child: Row(
                    children: [
                      Icon(Icons.search, color: Colors.grey[600], size: 20),
                      SizedBox(width: 12),
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
                SizedBox(height: 20),

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
                          margin: EdgeInsets.only(right: 16),
                          child: Column(
                            children: [
                              Container(
                                width: 60,
                                height: 60,
                                decoration: BoxDecoration(
                                  shape: BoxShape.circle,
                                  color: isSelected ? Colors.blue : Colors.grey[200],
                                  border: isSelected
                                      ? Border.all(color: Colors.blue, width: 3)
                                      : null,
                                ),
                                child: Center(
                                  child: category == '전체'
                                      ? Text(
                                    '전체',
                                    style: TextStyle(
                                      color: isSelected ? Colors.white : Colors.black,
                                      fontSize: 12,
                                      fontWeight: FontWeight.bold,
                                    ),
                                  )
                                      : Container(
                                    width: 50,
                                    height: 50,
                                    child: ClipOval(
                                      child: Image.asset(
                                        imagePath,
                                        fit: BoxFit.cover,
                                        errorBuilder: (context, error, stackTrace) {
                                          return Container(
                                            decoration: BoxDecoration(
                                              shape: BoxShape.circle,
                                              color: Colors.grey[300],
                                            ),
                                            child: Center(
                                              child: Text(
                                                category,
                                                style: TextStyle(
                                                  color: Colors.black,
                                                  fontSize: 10,
                                                  fontWeight: FontWeight.bold,
                                                ),
                                              ),
                                            ),
                                          );
                                        },
                                      ),
                                    ),
                                  ),
                                ),
                              ),
                              SizedBox(height: 8),
                              Text(
                                label,
                                style: TextStyle(
                                  fontSize: 12,
                                  color: isSelected ? Colors.blue : Colors.black,
                                  fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                                ),
                              ),
                            ],
                          ),
                        ),
                      );
                    }),
                  ),
                ),
                SizedBox(height: 20),

                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      selectedCategory == '전체'
                          ? 'OOO님을 위한 추천 상품'
                          : '상품 목록 (${filteredProducts.length}개)',
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                        color: Colors.black,
                      ),
                    ),
                    if (isLoading)
                      SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      ),
                  ],
                ),
                SizedBox(height: 16),

                if (errorMessage.isNotEmpty)
                  Container(
                    width: double.infinity,
                    padding: EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: Colors.blue[50],
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: Colors.blue[200]!),
                    ),
                    child: Column(
                      children: [
                        Text(
                          '데이터를 불러오는 중 오류가 발생했습니다',
                          style: TextStyle(color: Colors.blue[700], fontWeight: FontWeight.bold),
                        ),
                        SizedBox(height: 8),
                        Text(
                          errorMessage,
                          style: TextStyle(color: Colors.blue[600], fontSize: 12),
                        ),
                        SizedBox(height: 12),
                        ElevatedButton(
                          onPressed: loadProducts,
                          child: Text('다시 시도'),
                          style: ElevatedButton.styleFrom(
                            backgroundColor: Colors.blue,
                            foregroundColor: Colors.white,
                          ),
                        ),
                      ],
                    ),
                  ),

                if (!isLoading && errorMessage.isEmpty)
                  filteredProducts.isEmpty
                      ? Container(
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
                    physics: NeverScrollableScrollPhysics(),
                    gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
                      crossAxisCount: 2,
                      crossAxisSpacing: 16,
                      mainAxisSpacing: 20,
                      childAspectRatio: 0.7,
                    ),
                    itemCount: filteredProducts.length,
                    itemBuilder: (context, index) {
                      return ProductCard(product: filteredProducts[index]);
                    },
                  ),

                if (isLoading && errorMessage.isEmpty)
                  Container(
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
                              color: Colors.grey[600],
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),

                SizedBox(height: 20),
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
    Key? key,
    required this.product,
  }) : super(key: key);

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
            offset: Offset(0, 1),
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
                borderRadius: BorderRadius.vertical(top: Radius.circular(8)),
              ),
              child: product.spurl.isEmpty
                  ? Center(
                child: Icon(
                  Icons.image,
                  color: Colors.grey[400],
                  size: 40,
                ),
              )
                  : ClipRRect(
                borderRadius: BorderRadius.vertical(top: Radius.circular(8)),
                child: Image.network(
                  product.spurl,
                  fit: BoxFit.cover,
                  loadingBuilder: (context, child, loadingProgress) {
                    if (loadingProgress == null) return child;
                    return Center(
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        value: loadingProgress.expectedTotalBytes != null
                            ? loadingProgress.cumulativeBytesLoaded /
                            loadingProgress.expectedTotalBytes!
                            : null,
                      ),
                    );
                  },
                  errorBuilder: (context, error, stackTrace) {
                    return Center(
                      child: Icon(
                        Icons.broken_image,
                        color: Colors.grey[400],
                        size: 40,
                      ),
                    );
                  },
                ),
              ),
            ),
          ),

          Expanded(
            flex: 3,
            child: Padding(
              padding: EdgeInsets.all(12),
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
                      SizedBox(width: 4),
                      Text(
                        '(${product.spreviews})',
                        style: TextStyle(
                          fontSize: 11,
                          color: Colors.grey[600],
                        ),
                      ),
                    ],
                  ),
                  SizedBox(height: 4),

                  Text(
                    product.spname,
                    style: TextStyle(
                      fontSize: 13,
                      fontWeight: FontWeight.bold,
                      color: Colors.black,
                    ),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                  SizedBox(height: 4),

                  Text(
                    '${product.spprice.toStringAsFixed(2)} ${product.spcurrency}',
                    style: TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.bold,
                      color: Colors.blue,
                    ),
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