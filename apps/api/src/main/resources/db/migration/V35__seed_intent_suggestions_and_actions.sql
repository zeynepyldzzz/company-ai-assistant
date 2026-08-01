-- A-22 (#141): vitrin sorulari, yonlendirme butonlari ve guncellenmis selamlama metni.
--
-- BUTON ILKESI: buton her intent'te olmaz. Buton, chatbot'un SINIRA DAYANDIGI yerde
-- anlamlidir — kestigi ("ve 29 kisi daha"), dar kapsam verdigi (bu haftanin menusu) ya da
-- zaten yonlendirdigi (arac rezervasyonu) noktalarda. Cevabin tam oldugu yerde buton
-- koymak ekrani gurultuye bogar ve kullaniciya "demek eksik cevap verdi" izlenimi verir.
-- Bu yuzden prosedur intent'leri (izin/fazla mesai/oryantasyon/mazeret) buton ALMAZ:
-- yanit adimlari, sorumlu departmani ve iletisimi zaten iceriyor.
--
-- duyurular BUTONSUZ: calisana acik bir duyuru ekrani henuz yok (yalnizca
-- /admin/announcements). Ekran eklendiginde action_target/label ayri migration'da doldurulur.
--
-- calisma_duzeni hedefi directory_employees secildi (my_schedule degil): bu intent iki
-- soruyu birden karsiliyor ve somut boslugu olan taraf "kimler ofiste" — A-20'de konulan
-- 25 kisilik liste siniri orada. /my-schedule ekraninin chatbot'a gore tek ustunlugu
-- duzenleme yapabilmek ve o ekran da yalnizca icinde bulunulan haftayi gosteriyor, yani
-- "gecmis haftalar" sinirini cozmuyor. Sabit hedefin bilinen zaafi: "yarin ofiste miyim"
-- yanitinin altinda bu buton alakasiz duracak.

-- --- Vitrin sorulari (karsilama chip'leri) -------------------------------------------
-- Sira en sik kullanilana gore. Ozel isim gerektiren intent'ler (rehber_kisi) vitrine
-- cikmaz: "Ayse Kaya'nin dahilisi" diye ornek gostermek tuhaf olur.
UPDATE intents SET suggested_question = 'Bugün yemekte ne var?',        suggestion_order = 1 WHERE name = 'yemek_menusu';
UPDATE intents SET suggested_question = 'Servisim kaçta?',              suggestion_order = 2 WHERE name = 'servis_saatleri';
UPDATE intents SET suggested_question = 'Bu hafta çalışma düzenim nedir?', suggestion_order = 3 WHERE name = 'calisma_duzeni';
UPDATE intents SET suggested_question = 'Son duyurular neler?',         suggestion_order = 4 WHERE name = 'duyurular';
UPDATE intents SET suggested_question = 'Aktif anketler neler?',        suggestion_order = 5 WHERE name = 'anket';
UPDATE intents SET suggested_question = 'Yıllık izin nasıl alınır?',    suggestion_order = 6 WHERE name = 'izin_prosedur';

-- --- Yonlendirme butonlari -----------------------------------------------------------
-- action_target SEMANTIK hedeftir, web URL'i DEGIL: frontend kendi route'una cevirir.
-- Faz 2'de mobil ayni yaniti kullanacak ve URL dondurmek onu kirardi.
UPDATE intents SET action_target = 'menu',                  action_label = 'Aylık menüyü gör'    WHERE name = 'yemek_menusu';
UPDATE intents SET action_target = 'shuttle_routes',        action_label = 'Servis hatları'      WHERE name IN ('servis_saatleri', 'servis_guzergah');
UPDATE intents SET action_target = 'directory_employees',   action_label = 'Tüm çalışanları gör' WHERE name = 'calisma_duzeni';
UPDATE intents SET action_target = 'directory_employees',   action_label = 'Rehberde ara'        WHERE name = 'rehber_kisi';
UPDATE intents SET action_target = 'directory_departments', action_label = 'Departmanlar'        WHERE name = 'rehber_departman';
UPDATE intents SET action_target = 'dashboard',             action_label = 'Anketlere git'       WHERE name = 'anket';
UPDATE intents SET action_target = 'vehicles',              action_label = 'Araç rezervasyonu'   WHERE name = 'arac_rezervasyon';

-- --- Selamlama metni -----------------------------------------------------------------
-- Karsilama mesaji AYNI kaynaktan beslenir: "merhaba" yanitiyla acilis metni ayni seydir,
-- iki yerde tutulmaz. Eski metin A-11 oncesinden kalmaydi ve eklenen yeteneklerin
-- (servis, calisma duzeni, rehber, duyuru/anket) hicbirinden haberi yoktu.
UPDATE response_templates rt
SET template = 'Merhaba {{kullanici_adi}}! Sana şu konularda yardımcı olabilirim:' || E'\n\n'
            || '• Yemek menüsü' || E'\n'
            || '• Servis saatleri ve güzergâhları' || E'\n'
            || '• Çalışma düzenin ve kimlerin ofiste olduğu' || E'\n'
            || '• Çalışan rehberi ve departman bilgileri' || E'\n'
            || '• Duyurular ve anketler' || E'\n'
            || '• İzin, fazla mesai ve işe giriş prosedürleri',
    updated_at = now()
FROM intents i
WHERE i.id = rt.intent_id AND i.name = 'selamlama';

-- --- Dogrulama ------------------------------------------------------------------------
-- Seed sessizce eksik uygulanmaz: beklenen intent adlarindan biri yoksa (yeniden
-- adlandirma, silme) migration durur.
DO $$
DECLARE
    v_missing TEXT;
    v_suggestions INTEGER;
BEGIN
    SELECT string_agg(p.name, ', ') INTO v_missing
    FROM (VALUES
        ('yemek_menusu'), ('servis_saatleri'), ('servis_guzergah'), ('calisma_duzeni'),
        ('rehber_kisi'), ('rehber_departman'), ('duyurular'), ('anket'),
        ('arac_rezervasyon'), ('izin_prosedur'), ('selamlama')
    ) AS p(name)
    WHERE NOT EXISTS (SELECT 1 FROM intents i WHERE i.name = p.name);

    IF v_missing IS NOT NULL THEN
        RAISE EXCEPTION 'Intent bulunamadi: % — vitrin/buton seed''i eksik uygulandi', v_missing;
    END IF;

    SELECT count(*) INTO v_suggestions FROM intents WHERE suggested_question IS NOT NULL;
    IF v_suggestions <> 6 THEN
        RAISE EXCEPTION 'Beklenen 6 vitrin sorusu, bulunan %', v_suggestions;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM response_templates rt
        JOIN intents i ON i.id = rt.intent_id
        WHERE i.name = 'selamlama' AND rt.template LIKE '%Servis saatleri%'
    ) THEN
        RAISE EXCEPTION 'selamlama template guncellenemedi — response_templates satiri yok';
    END IF;
END $$;
