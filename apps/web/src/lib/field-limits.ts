/**
 * A-44 (#219): form alanlarının karakter sınırları.
 *
 * SAYILARIN KAYNAĞI migration'lardaki kolon genişlikleri; backend DTO'larındaki `@Size`
 * değerleriyle birebir aynı olmalı. Burada tutulmalarının sebebi arayüz tarafında tek kaynak
 * sağlamak — sayıyı forma gömersek biri değişince diğeri unutulur.
 *
 * Aynı sayı üç yerde yaşıyor (migration, DTO, burası). Tek kaynağa indirmek kod üretimi ya da
 * paylaşılan bir şema gerektirir; #219'un kapsamı dışında bırakıldı. Bir sınır değişecekse
 * üçünün birlikte değişmesi gerekiyor.
 *
 * TEXT kolonlarında (duyuru metni, sorumluluklar) veritabanı sınırlamıyor; 2000 ürün kararı.
 *
 * Telefon alanları BİLEREK veritabanından dar (20 < 30/50). Arayüzün API'den katı olması
 * güvenli yön — tersi değil. Bu değerler #183'te seçilmişti, gevşetmek için sebep yok.
 */
export const FIELD_LIMITS = {
  // employee — V50: first_name/last_name VARCHAR(100), V1: email VARCHAR(150), phone VARCHAR(30)
  employeeFirstName: 100,
  employeeLastName: 100,
  employeeEmail: 150,
  /** DB 30'a izin veriyor; 20 arayüz kararı (#183). */
  employeePhone: 20,

  // department — V1: name VARCHAR(150), responsibilities TEXT
  departmentName: 150,
  departmentResponsibilities: 2000,

  // vehicle — V1: plate VARCHAR(20), model VARCHAR(100)
  vehiclePlate: 20,
  vehicleModel: 100,

  // shuttle — V1: route.name VARCHAR(150), plate_number VARCHAR(20), stop.name VARCHAR(150)
  //           V29: driver_name VARCHAR(255), driver_phone VARCHAR(50)
  shuttleRouteName: 150,
  /** DB 20'ye izin veriyor; 12 plaka biçimi için yeterli (#183). */
  shuttlePlateNumber: 12,
  shuttleDriverName: 255,
  /** DB 50'ye izin veriyor; 20 arayüz kararı (#183). */
  shuttleDriverPhone: 20,
  shuttleStopName: 150,

  // announcement — V1: title VARCHAR(255), content TEXT
  announcementTitle: 255,
  announcementContent: 2000,

  // survey — V1: title VARCHAR(255), V40: survey_option.option_text VARCHAR(255)
  surveyTitle: 255,
  surveyOption: 255,

  // NOT: meal_item.name (V1, VARCHAR(150)) burada YOK — menü yönetimi metin girişi
  // kullanmıyor, yemek adları Excel yüklemesinden geliyor. Sınır backend DTO'sunda duruyor.
} as const;
