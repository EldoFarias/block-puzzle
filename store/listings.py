"""Store listing texts and screenshot captions in the 8 languages of the game.

Writes store/listings/<play-locale>/{title,short_description,full_description}.txt and checks the
Play Console limits (30 / 80 / 4000 characters). Captions are used by store/make_screenshots.py.

    python store/listings.py
"""
import os

HERE = os.path.dirname(os.path.abspath(__file__))

L = {}

L["en-US"] = dict(
    title="Prisma Blocks: Block Puzzle",
    short="Relaxing 8x8 block puzzle with jewel blocks, combos and an endless adventure.",
    full="""Drag the blocks, fill rows and columns and watch them burst into light. Prisma Blocks is a calm, elegant block puzzle that is easy to learn and hard to put down.

★ CLASSIC MODE
• 8x8 board, three pieces at a time, no rotation: simple rules, deep strategy
• Clear several lines at once and chain moves to build huge COMBOS
• Empty the whole board for a big bonus
• Your game is saved automatically, so you can stop and continue anytime

★ ENDLESS ADVENTURE
• Worlds full of levels with new goals: collect gems, melt ice, clear drawings, reach combos
• Every world brings a new twist: stones that need two hits, ice, locked cells and time bombs
• A boss every 10 levels with two goals at once
• Chests along the trail and a star chest for every 30 stars

★ EVERY DAY
• Daily challenge with a streak to keep alive
• Three daily missions with coin rewards
• A daily chest that grows over 7 days

★ MADE TO FEEL GOOD
• Glossy jewel blocks, soft lights and smooth animations
• 6 themes: Neon, Ocean, Candy, Lava, Galaxy and Gold
• Power-ups when you need them: undo, bomb and shuffle
• Relaxing music, satisfying sounds and vibration (all optional)
• Light on your battery and works offline

Free to play, with optional ads. No account needed.
""",
    captions=["Clear lines, chain combos", "An endless adventure", "Clear the drawings",
              "New worlds, new challenges", "Beat the bosses", "Daily challenge and missions", "6 beautiful themes"],
)

L["pt-BR"] = dict(
    title="Prisma Blocks: Jogo de Blocos",
    short="Quebra-cabeça de blocos 8x8 relaxante, com blocos de joia, combos e aventura.",
    full="""Arraste os blocos, complete linhas e colunas e veja tudo explodir em luz. Prisma Blocks é um quebra-cabeça de blocos calmo e elegante, fácil de aprender e difícil de largar.

★ MODO CLÁSSICO
• Tabuleiro 8x8, três peças por vez, sem girar: regras simples, estratégia profunda
• Limpe várias linhas de uma vez e encadeie jogadas para fazer COMBOS enormes
• Esvazie o tabuleiro inteiro para ganhar um bônus
• A partida é salva sozinha: pare e continue quando quiser

★ AVENTURA SEM FIM
• Mundos cheios de fases com novos objetivos: colete gemas, derreta gelo, limpe desenhos, faça combos
• Cada mundo traz uma novidade: pedras que precisam de duas limpezas, gelo, cadeados e bombas-relógio
• Um chefe a cada 10 fases, com dois objetivos ao mesmo tempo
• Baús pela trilha e um baú de estrelas a cada 30 estrelas

★ TODO DIA
• Desafio diário com sequência de dias para manter
• Três missões diárias com recompensa em moedas
• Baú diário que cresce ao longo de 7 dias

★ FEITO PARA SER GOSTOSO DE JOGAR
• Blocos brilhantes de joia, luzes suaves e animações fluidas
• 6 temas: Neon, Oceano, Doces, Lava, Galáxia e Ouro
• Poderes quando você precisar: desfazer, bomba e trocar
• Música relaxante, sons satisfatórios e vibração (tudo opcional)
• Leve para a bateria e funciona sem internet

Gratuito, com anúncios opcionais. Não precisa de conta.
""",
    captions=["Limpe linhas, faça combos", "Uma aventura sem fim", "Limpe os desenhos",
              "Novos mundos, novos desafios", "Derrote os chefes", "Desafio diário e missões", "6 temas lindos"],
)

L["es-ES"] = dict(
    title="Prisma Blocks: Puzzle Bloques",
    short="Puzzle de bloques 8x8 relajante, con bloques de joya, combos y una aventura.",
    full="""Arrastra los bloques, completa filas y columnas y míralas estallar en luz. Prisma Blocks es un puzzle de bloques tranquilo y elegante, fácil de aprender y difícil de dejar.

★ MODO CLÁSICO
• Tablero 8x8, tres piezas a la vez, sin girar: reglas simples, estrategia profunda
• Limpia varias líneas a la vez y encadena jugadas para hacer COMBOS enormes
• Vacía todo el tablero para ganar un bono
• La partida se guarda sola: para y continúa cuando quieras

★ AVENTURA SIN FIN
• Mundos llenos de niveles con nuevos objetivos: recoge gemas, derrite hielo, limpia dibujos, logra combos
• Cada mundo trae una novedad: piedras que necesitan dos golpes, hielo, candados y bombas de tiempo
• Un jefe cada 10 niveles, con dos objetivos a la vez
• Cofres en el camino y un cofre de estrellas cada 30 estrellas

★ CADA DÍA
• Desafío diario con racha de días que mantener
• Tres misiones diarias con monedas de recompensa
• Cofre diario que crece durante 7 días

★ HECHO PARA DISFRUTAR
• Bloques brillantes de joya, luces suaves y animaciones fluidas
• 6 temas: Neón, Océano, Dulces, Lava, Galaxia y Oro
• Poderes cuando los necesites: deshacer, bomba y cambiar
• Música relajante, sonidos satisfactorios y vibración (todo opcional)
• Ligero para la batería y funciona sin internet

Gratis, con anuncios opcionales. No necesitas cuenta.
""",
    captions=["Limpia líneas, haz combos", "Una aventura sin fin", "Limpia los dibujos",
              "Nuevos mundos, nuevos retos", "Vence a los jefes", "Desafío diario y misiones", "6 temas preciosos"],
)

L["fr-FR"] = dict(
    title="Prisma Blocks : Puzzle blocs",
    short="Puzzle de blocs 8x8 relaxant, avec blocs de joyaux, combos et une aventure.",
    full="""Glissez les blocs, complétez lignes et colonnes et regardez-les éclater en lumière. Prisma Blocks est un puzzle de blocs calme et élégant, facile à apprendre et difficile à lâcher.

★ MODE CLASSIQUE
• Plateau 8x8, trois pièces à la fois, sans rotation : règles simples, stratégie profonde
• Effacez plusieurs lignes d'un coup et enchaînez pour faire d'énormes COMBOS
• Videz tout le plateau pour gagner un bonus
• Votre partie est sauvegardée toute seule : arrêtez et reprenez quand vous voulez

★ AVENTURE SANS FIN
• Des mondes remplis de niveaux aux nouveaux objectifs : gemmes, glace à faire fondre, dessins à effacer, combos
• Chaque monde apporte une nouveauté : pierres à frapper deux fois, glace, cadenas et bombes à retardement
• Un boss tous les 10 niveaux, avec deux objectifs à la fois
• Des coffres sur le chemin et un coffre d'étoiles toutes les 30 étoiles

★ CHAQUE JOUR
• Défi du jour avec une série de jours à entretenir
• Trois missions quotidiennes récompensées en pièces
• Un coffre quotidien qui grandit sur 7 jours

★ FAIT POUR SE FAIRE PLAISIR
• Blocs brillants comme des joyaux, lumières douces et animations fluides
• 6 thèmes : Néon, Océan, Bonbons, Lave, Galaxie et Or
• Des bonus quand il le faut : annuler, bombe et changer
• Musique relaxante, sons satisfaisants et vibration (tout est optionnel)
• Léger pour la batterie et jouable hors ligne

Gratuit, avec publicités optionnelles. Aucun compte nécessaire.
""",
    captions=["Effacez des lignes, enchaînez", "Une aventure sans fin", "Effacez les dessins",
              "Nouveaux mondes, nouveaux défis", "Battez les boss", "Défi du jour et missions", "6 magnifiques thèmes"],
)

L["de-DE"] = dict(
    title="Prisma Blocks: Block Puzzle",
    short="Entspanntes 8x8 Block Puzzle mit Juwelen-Blöcken, Combos und Abenteuer.",
    full="""Ziehe die Blöcke, fülle Reihen und Spalten und sieh zu, wie sie in Licht zerspringen. Prisma Blocks ist ein ruhiges, elegantes Block Puzzle: schnell gelernt und schwer wegzulegen.

★ KLASSISCHER MODUS
• 8x8 Spielfeld, drei Teile auf einmal, kein Drehen: einfache Regeln, viel Strategie
• Räume mehrere Reihen gleichzeitig ab und verkette Züge zu riesigen COMBOS
• Leere das ganze Feld für einen dicken Bonus
• Dein Spiel wird automatisch gespeichert: jederzeit aufhören und weitermachen

★ ENDLOSES ABENTEUER
• Welten voller Level mit neuen Zielen: Edelsteine sammeln, Eis schmelzen, Zeichnungen abräumen, Combos schaffen
• Jede Welt bringt etwas Neues: Steine, die zwei Treffer brauchen, Eis, Schlösser und Zeitbomben
• Alle 10 Level ein Boss mit zwei Zielen gleichzeitig
• Truhen am Weg und eine Sternentruhe alle 30 Sterne

★ JEDEN TAG
• Tägliche Herausforderung mit einer Serie, die du halten kannst
• Drei tägliche Missionen mit Münzen als Belohnung
• Eine tägliche Truhe, die über 7 Tage wächst

★ GEMACHT ZUM WOHLFÜHLEN
• Glänzende Juwelen-Blöcke, sanftes Licht und flüssige Animationen
• 6 Themen: Neon, Ozean, Bonbon, Lava, Galaxie und Gold
• Hilfen, wenn du sie brauchst: Rückgängig, Bombe und Tauschen
• Entspannte Musik, befriedigende Sounds und Vibration (alles optional)
• Schont den Akku und funktioniert offline

Kostenlos, mit optionaler Werbung. Kein Konto nötig.
""",
    captions=["Reihen abräumen, Combos", "Ein endloses Abenteuer", "Zeichnungen abräumen",
              "Neue Welten, neue Rätsel", "Besiege die Bosse", "Tägliche Missionen", "6 schöne Themen"],
)

L["tr-TR"] = dict(
    title="Prisma Blocks: Blok Bulmaca",
    short="Mücevher bloklar, kombolar ve sonsuz macerayla rahatlatıcı 8x8 blok bulmaca.",
    full="""Blokları sürükle, satır ve sütunları doldur ve ışığa dönüşmelerini izle. Prisma Blocks sakin ve şık bir blok bulmaca: öğrenmesi kolay, bırakması zor.

★ KLASİK MOD
• 8x8 tahta, aynı anda üç parça, döndürme yok: basit kurallar, derin strateji
• Aynı anda birden fazla satır temizle ve hamleleri zincirleyip büyük KOMBOLAR yap
• Tüm tahtayı boşalt ve büyük bonus kazan
• Oyunun otomatik kaydedilir: istediğin zaman bırak, sonra devam et

★ SONSUZ MACERA
• Yeni hedeflerle dolu dünyalar: mücevher topla, buzu erit, çizimleri temizle, kombo yap
• Her dünya bir yenilik getirir: iki kez vurulması gereken taşlar, buz, kilitler ve saatli bombalar
• Her 10 bölümde bir, aynı anda iki hedefli boss
• Yol boyunca sandıklar ve her 30 yıldızda bir yıldız sandığı

★ HER GÜN
• Serini koruyabileceğin günlük meydan okuma
• Jeton ödüllü üç günlük görev
• 7 gün boyunca büyüyen günlük sandık

★ KEYİFLİ OLSUN DİYE YAPILDI
• Parlak mücevher bloklar, yumuşak ışıklar ve akıcı animasyonlar
• 6 tema: Neon, Okyanus, Şeker, Lav, Galaksi ve Altın
• İhtiyaç duyduğunda güçler: geri al, bomba ve değiştir
• Rahatlatıcı müzik, tatmin edici sesler ve titreşim (hepsi isteğe bağlı)
• Pili yormaz ve internetsiz çalışır

Ücretsiz, isteğe bağlı reklamlarla. Hesap gerekmez.
""",
    captions=["Satır temizle, kombo yap", "Sonsuz bir macera", "Çizimleri temizle",
              "Yeni dünyalar, yeni zorluklar", "Boss'ları yen", "Günlük görevler", "6 güzel tema"],
)

L["id"] = dict(
    title="Prisma Blocks: Puzzle Blok",
    short="Puzzle blok 8x8 yang santai dengan blok permata, kombo, dan petualangan.",
    full="""Seret blok, penuhi baris dan kolom, lalu lihat semuanya pecah menjadi cahaya. Prisma Blocks adalah puzzle blok yang tenang dan elegan: mudah dipelajari, sulit berhenti.

★ MODE KLASIK
• Papan 8x8, tiga keping sekaligus, tanpa memutar: aturan sederhana, strategi mendalam
• Hapus beberapa baris sekaligus dan rangkai langkah untuk KOMBO besar
• Kosongkan seluruh papan untuk bonus besar
• Permainan tersimpan otomatis: berhenti dan lanjutkan kapan saja

★ PETUALANGAN TANPA AKHIR
• Dunia penuh level dengan tujuan baru: kumpulkan permata, cairkan es, hapus gambar, raih kombo
• Setiap dunia membawa hal baru: batu yang perlu dua kali dihapus, es, gembok, dan bom waktu
• Bos setiap 10 level dengan dua tujuan sekaligus
• Peti di sepanjang jalur dan peti bintang setiap 30 bintang

★ SETIAP HARI
• Tantangan harian dengan rangkaian hari yang bisa kamu jaga
• Tiga misi harian dengan hadiah koin
• Peti harian yang bertambah selama 7 hari

★ DIBUAT AGAR MENYENANGKAN
• Blok permata berkilau, cahaya lembut, dan animasi halus
• 6 tema: Neon, Samudra, Permen, Lava, Galaksi, dan Emas
• Bantuan saat dibutuhkan: urungkan, bom, dan ganti
• Musik santai, suara memuaskan, dan getaran (semua opsional)
• Hemat baterai dan bisa dimainkan offline

Gratis, dengan iklan opsional. Tidak perlu akun.
""",
    captions=["Hapus baris, buat kombo", "Petualangan tanpa akhir", "Hapus gambarnya",
              "Dunia baru, tantangan baru", "Kalahkan para bos", "Tantangan & misi harian", "6 tema indah"],
)

L["hi-IN"] = dict(
    title="Prisma Blocks: ब्लॉक पज़ल",
    short="रत्न जैसे ब्लॉक, कॉम्बो और अंतहीन एडवेंचर वाला सुकून भरा 8x8 ब्लॉक पज़ल।",
    full="""ब्लॉक खींचें, पंक्तियाँ और कॉलम भरें और उन्हें रोशनी में बिखरते देखें। Prisma Blocks एक शांत और शानदार ब्लॉक पज़ल है: सीखना आसान, छोड़ना मुश्किल।

★ क्लासिक मोड
• 8x8 बोर्ड, एक बार में तीन टुकड़े, घुमाना नहीं: आसान नियम, गहरी रणनीति
• एक साथ कई लाइनें साफ़ करें और चालें जोड़कर बड़े COMBO बनाएँ
• पूरा बोर्ड खाली करें और बड़ा बोनस पाएँ
• आपका खेल अपने आप सेव होता है: कभी भी रुकें और फिर जारी रखें

★ अंतहीन एडवेंचर
• नए लक्ष्यों वाले लेवल से भरी दुनियाएँ: रत्न इकट्ठा करें, बर्फ़ पिघलाएँ, चित्र साफ़ करें, कॉम्बो बनाएँ
• हर दुनिया कुछ नया लाती है: दो बार तोड़े जाने वाले पत्थर, बर्फ़, ताले और टाइम बम
• हर 10 लेवल पर एक बॉस, एक साथ दो लक्ष्यों के साथ
• रास्ते में संदूक और हर 30 सितारों पर सितारों का संदूक

★ हर दिन
• रोज़ाना चैलेंज और लगातार दिनों की स्ट्रीक
• सिक्कों के इनाम वाले तीन रोज़ाना मिशन
• 7 दिनों में बढ़ने वाला रोज़ाना संदूक

★ मज़ेदार बनाने के लिए
• चमकदार रत्न ब्लॉक, हल्की रोशनी और स्मूथ एनिमेशन
• 6 थीम: नियॉन, समुद्र, कैंडी, लावा, गैलेक्सी और सोना
• ज़रूरत पड़ने पर पावर-अप: वापस लें, बम और बदलें
• सुकून भरा संगीत, मज़ेदार आवाज़ें और वाइब्रेशन (सब वैकल्पिक)
• बैटरी पर हल्का और बिना इंटरनेट के चलता है

मुफ़्त, वैकल्पिक विज्ञापनों के साथ। किसी अकाउंट की ज़रूरत नहीं।
""",
    captions=["लाइनें साफ़ करें, कॉम्बो बनाएँ", "एक अंतहीन एडवेंचर", "चित्र साफ़ करें",
              "नई दुनिया, नई चुनौतियाँ", "बॉस को हराएँ", "रोज़ाना चैलेंज और मिशन", "6 सुंदर थीम"],
)

# Emulator locale used for each listing's screenshots.
APP_LOCALE = {"en-US": "en-US", "pt-BR": "pt-BR", "es-ES": "es-ES", "fr-FR": "fr-FR", "de-DE": "de-DE",
              "tr-TR": "tr-TR", "id": "id-ID", "hi-IN": "hi-IN"}

if __name__ == "__main__":
    for loc, t in L.items():
        for key, limit in (("title", 30), ("short", 80), ("full", 4000)):
            n = len(t[key].strip())
            assert n <= limit, (loc, key, n, limit)
        assert len(t["captions"]) == 7, loc
        d = os.path.join(HERE, "listings", loc)
        os.makedirs(d, exist_ok=True)
        for key, name in (("title", "title"), ("short", "short_description"), ("full", "full_description")):
            with open(os.path.join(d, name + ".txt"), "w", encoding="utf-8", newline="\n") as f:
                f.write(t[key].strip() + "\n")
        print(f"{loc:6} title={len(t['title']):2} short={len(t['short']):2} full={len(t['full'].strip()):4}")
