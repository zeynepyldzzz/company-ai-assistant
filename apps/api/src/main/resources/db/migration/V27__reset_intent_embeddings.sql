-- A-17 (#124): intent embedding'leri yeniden hesaplanmak uzere sifirlanir.
--
-- Sebep: sorgu ve seed metinleri normalizasyondan gecmiyordu. Olculen etki (chat_message_log):
--   "selamlar" 0.797 eslesti  /  "Selamlar" 0.513 eslesmedi
--   "selam"    0.787 eslesti  /  "Selam"    0.512 eslesmedi
-- Ayni kelime, yalnizca ilk harf farki. V8'deki ornek cumlelerin bir kismi da buyuk harfle
-- basliyor ("Ahmet Beyin dahili numarasi kac"), yani tutarsizlik iki tarafta birden.
--
-- IntentClassificationService ve IntentSeedRunner artik TurkishText.normalizeForEmbedding
-- kullaniyor. Mevcut embedding'ler HAM metinden hesaplandigi icin gecersiz; IntentSeedRunner
-- yalnizca 'embedding IS NULL' satirlari doldurdugundan burada sifirlaniyor.
--
-- DIKKAT: bu migration'dan sonra ilk acilista Ollama erisilebilir olmali, aksi halde
-- embedding'ler NULL kalir ve hicbir intent eslesmez (IntentClassificationService NULL
-- embedding'leri sorgu disi birakir).

UPDATE intent_examples SET embedding = NULL;
