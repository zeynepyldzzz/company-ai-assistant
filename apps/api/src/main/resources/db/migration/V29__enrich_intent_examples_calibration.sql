-- A-19 (#129): intent ornek zenginlestirmesi.
--
-- Olcum (IntentCalibrationIT taban cizgisi 17/26, esik 0.68): basarisiz sorgularin
-- HEPSINDE en yakin cumle DOGRU intent'e ait, yalnizca skor esigin altinda kaliyor.
-- Yani siralama saglam, eksik olan bicim cesitliligi:
--   "anketler" 0.675 | "kimler ofiste" 0.498 | "muhasebe bolumu" 0.545
--   "kadikoy servisi" 0.572 | "carsamba servisi kacta" 0.671 | "calisma duzenim" 0.663
--
-- Esik dusurmek COZUM DEGIL: "carsamba" tek basina 0.611 ile selamlamaya yakin ve
-- eslesmemesi gerekiyor. Dogru mudahale, dogru sorgularin skorunu yukari cekmek.
--
-- YONTEM KURALI: test setindeki sorgular buraya AYNEN eklenmez. Eklenirse test 1.000
-- benzerlikle gecer ve hicbir sey olcmus olmayiz. Ornekler ayni KALIBI temsil eder,
-- ayni cumleyi degil (test: "carsamba servisi kacta" -> ornek: "sali servisi kacta").
--
-- Embedding NULL birakilir; IntentSeedRunner acilista doldurur (A-17'den beri
-- TurkishText.normalizeForEmbedding ile). Acilista Ollama erisilebilir olmali.

INSERT INTO intent_examples (intent_id, phrase)
SELECT i.id, p.phrase
FROM intents i
JOIN (VALUES
    -- anket: kisa/cogul formlar ("anketler" 0.675 ile kil payi kaciriyordu)
    ('anket', 'anket listesi'),
    ('anket', 'hangi anketler açık'),
    ('anket', 'katılabileceğim anketler neler'),

    -- duyurular: kisa form destegi (0.748 ile geciyor ama tek ornek kalibi zayif)
    ('duyurular', 'son duyurular neler'),
    ('duyurular', 'duyuru var mı'),

    -- servis_saatleri: gun nitelemeli kalip. NOBETCI "carsamba servisi kacta" 0.671 ile
    -- dusuyordu; menu tarafi ayni kalibi 0.737 ile geciyor, denge kuruluyor.
    ('servis_saatleri', 'salı servisi kaçta'),
    ('servis_saatleri', 'servis saatleri neler'),
    ('servis_saatleri', 'pazartesi servisin kalkış saati'),

    -- servis_guzergah: yer adi iceren kisa formlar ("kadikoy servisi" 0.572)
    ('servis_guzergah', 'bostancı servisi'),
    ('servis_guzergah', 'levent hattı hangi duraklardan geçiyor'),
    ('servis_guzergah', 'benim semtimden hangi servis geçiyor'),

    -- rehber_departman: "bolum" es anlamlisi ve yetkili sorusu (0.545 / 0.572)
    ('rehber_departman', 'pazarlama bölümünün yöneticisi kim'),
    ('rehber_departman', 'insan kaynakları birimi kime bağlı'),
    ('rehber_departman', 'departman sorumlusuna nasıl ulaşırım'),

    -- rehber_kisi: kisi adi + dahili kalibi. "X'in dahilisi" su an rehber_departman'a
    -- cekiliyor ("bilgi islemin dahilisi kac" orada); ad-soyadli ornekler dengeler.
    ('rehber_kisi', 'Mehmet Demir''in dahilisi kaç'),
    ('rehber_kisi', 'Zeynep Aydın''ın e-posta adresi nedir'),
    ('rehber_kisi', 'Elif Şahin hangi departmanda çalışıyor'),

    -- calisma_duzeni: ucuncu sahis ve kisa formlar ("kimler ofiste" 0.498 —
    -- ornegin tam alt-dizesi oldugu halde dusuk; kisa sorgu/uzun ornek etkisi)
    ('calisma_duzeni', 'kimler ofisten çalışıyor'),
    ('calisma_duzeni', 'kimler evden çalışıyor'),
    ('calisma_duzeni', 'çalışma planım nedir'),
    ('calisma_duzeni', 'bu hafta hangi günler ofisteyim')
) AS p(intent_name, phrase) ON i.name = p.intent_name;

-- Beklenen satir sayisi tutmuyorsa intent adlarindan biri hatali demektir; sessizce
-- eksik seed etmek yerine durulur (seed kurali).
DO $$
DECLARE
    v_missing TEXT;
BEGIN
    SELECT string_agg(DISTINCT p.intent_name, ', ') INTO v_missing
    FROM (VALUES
        ('anket'), ('duyurular'), ('servis_saatleri'), ('servis_guzergah'),
        ('rehber_departman'), ('rehber_kisi'), ('calisma_duzeni')
    ) AS p(intent_name)
    WHERE NOT EXISTS (SELECT 1 FROM intents i WHERE i.name = p.intent_name);

    IF v_missing IS NOT NULL THEN
        RAISE EXCEPTION 'Intent bulunamadi: % — ornek zenginlestirmesi eksik uygulandi', v_missing;
    END IF;
END $$;
