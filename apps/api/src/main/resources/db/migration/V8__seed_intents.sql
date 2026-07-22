-- V8__seed_intents.sql
-- Intent'ler ve örnek cümleler. Embedding'ler uygulama startup'ında
-- Ollama (bge-m3) ile hesaplanıp UPDATE edilir (IntentSeedRunner).
-- RAG işaretli intent'ler Faz 2'de RAG pipeline'ına yönlenecek;
-- şimdilik hepsi sınıflandırma kapsamında.

INSERT INTO intents (name, description) VALUES
    ('yemek_menusu',          'Günlük/haftalık yemek menüsü, kalori ve alerjen sorguları (template)'),
    ('servis_saatleri',       'Personel servisi kalkış saatleri (template)'),
    ('servis_guzergah',       'Servis güzergah, durak ve plaka bilgisi (template)'),
    ('rehber_kisi',           'Kişi arama: dahili, e-posta (template)'),
    ('rehber_departman',      'Departman iletişim bilgisi (template)'),
    ('duyurular',             'Şirket duyuruları (template)'),
    ('arac_rezervasyon',      'Havuz aracı rezervasyon süreci (template)'),
    ('anket',                 'Anketler ve anonim geri bildirim (template)'),
    ('calisma_duzeni',        'Ofis/uzaktan çalışma programı (template)'),
    ('izin_prosedur',         'İzin süreçleri — Faz 2''de RAG''e yönlenir'),
    ('fazla_mesai',           'Fazla mesai süreçleri — Faz 2''de RAG''e yönlenir'),
    ('ise_giris_oryantasyon', 'İşe giriş ve oryantasyon süreçleri — Faz 2''de RAG''e yönlenir'),
    ('selamlama',             'Selamlama ve nezaket ifadeleri (template)');

INSERT INTO intent_examples (intent_id, phrase)
SELECT i.id, p.phrase
FROM intents i
JOIN (VALUES
    ('yemek_menusu', 'bugün yemekte ne var'),
    ('yemek_menusu', 'öğle yemeği menüsü nedir'),
    ('yemek_menusu', 'bu haftanın yemek listesi'),
    ('yemek_menusu', 'yarın ne yiyeceğiz'),
    ('yemek_menusu', 'bugünkü yemekte gluten ya da alerjen var mı'),
    ('yemek_menusu', 'menüdeki yemeklerin kalorisi ne kadar'),

    ('servis_saatleri', 'servis kaçta kalkıyor'),
    ('servis_saatleri', 'akşam servisi saat kaçta'),
    ('servis_saatleri', 'sabah servisinin saati nedir'),

    ('servis_guzergah', 'servis nereden geçiyor'),
    ('servis_guzergah', 'Bornova servisi hangi duraklardan geçiyor'),
    ('servis_guzergah', 'bana en yakın servis durağı nerede'),
    ('servis_guzergah', 'servisin plakası ne'),

    ('rehber_kisi', 'Ahmet Beyin dahili numarası kaç'),
    ('rehber_kisi', 'muhasebeden Ayşe Hanımın e-postası nedir'),
    ('rehber_kisi', 'bir çalışanın telefon numarasını bulmak istiyorum'),

    ('rehber_departman', 'İK departmanının numarası ne'),
    ('rehber_departman', 'bilgi işlemin dahilisi kaç'),
    ('rehber_departman', 'satın alma departmanına nasıl ulaşırım'),

    ('duyurular', 'yeni duyuru var mı'),
    ('duyurular', 'son şirket duyurularını göster'),
    ('duyurular', 'bugün bir duyuru yayınlandı mı'),

    ('arac_rezervasyon', 'araç nasıl ayırtırım'),
    ('arac_rezervasyon', 'yarın için havuz aracı lazım'),
    ('arac_rezervasyon', 'şirket aracı rezervasyonu yapmak istiyorum'),

    ('anket', 'aktif anket var mı'),
    ('anket', 'doldurmam gereken anket kaldı mı'),
    ('anket', 'anketlere nereden ulaşırım'),
    ('anket', 'anonim geri bildirim nasıl gönderirim'),

    ('calisma_duzeni', 'bu hafta kimler ofiste'),
    ('calisma_duzeni', 'yarın ofise gelmem gerekiyor mu'),
    ('calisma_duzeni', 'uzaktan çalışma günlerim hangileri'),

    ('izin_prosedur', 'yıllık izin nasıl alınır'),
    ('izin_prosedur', 'mazeret izni için ne yapmam gerekiyor'),
    ('izin_prosedur', 'kaç gün izin hakkım var'),
    ('izin_prosedur', 'izin talebimi nereden oluşturabilirim'),

    ('fazla_mesai', 'fazla mesai ücreti nasıl hesaplanıyor'),
    ('fazla_mesai', 'mesaiye kalırsam ne yapmalıyım'),
    ('fazla_mesai', 'fazla mesai onayını kim veriyor'),

    ('ise_giris_oryantasyon', 'işe yeni başladım neleri bilmem gerekiyor'),
    ('ise_giris_oryantasyon', 'oryantasyon süreci nasıl işliyor'),
    ('ise_giris_oryantasyon', 'yeni çalışanlar için şirket kuralları neler'),
    ('ise_giris_oryantasyon', 'ilk gün ne yapmam lazım'),

    ('selamlama', 'merhaba'),
    ('selamlama', 'selam nasılsın'),
    ('selamlama', 'günaydın'),
    ('selamlama', 'teşekkür ederim')
) AS p(intent_name, phrase) ON p.intent_name = i.name;