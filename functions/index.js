const functions = require('firebase-functions');

/**
 * AkaTrade AI - Gemini API Proxy
 *
 * Bu Cloud Function, Android istemciden gelen GenerateContentRequest'i alır,
 * sunucu tarafındaki GEMINI_API_KEY'i ekleyerek Google Gemini API'ye iletir,
 * ve yanıtı olduğu gibi istemciye döndürür.
 *
 * API anahtarı hiçbir zaman mobil istemciye veya APK'ya dahil edilmez.
 *
 * Yapılandırma:
 *   firebase functions:config:set gemini.api_key="GERCEK_ANAHTAR"
 *   firebase deploy --only functions
 */
exports.generateContent = functions.https.onRequest(async (req, res) => {
  // === CORS başlıkları (AI Studio ve geliştirme için) ===
  res.set('Access-Control-Allow-Origin', '*');
  res.set('Access-Control-Allow-Methods', 'POST, OPTIONS');
  res.set('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    res.status(204).send('');
    return;
  }

  // === Sadece POST kabul edilir ===
  if (req.method !== 'POST') {
    res.status(405).json({ error: 'Method not allowed' });
    return;
  }

  // === İstek gövdesi validasyonu ===
  if (!req.body || !req.body.contents) {
    res.status(400).json({ error: 'Invalid request: missing "contents" field' });
    return;
  }

  // === Sunucu tarafı API anahtarı (environment variable) ===
  // Firebase deploy öncesinde functions/.env dosyasına GEMINI_API_KEY eklenmelidir:
  //   echo "GEMINI_API_KEY=ANAHTARINIZ" >> functions/.env
  const geminiApiKey = process.env.GEMINI_API_KEY;
  if (!geminiApiKey) {
    functions.logger.error(
      'GEMINI_API_KEY environment variable bulunamadi.',
      'functions/.env dosyasina GEMINI_API_KEY=ANAHTARINIZ ekleyin.'
    );
    res.status(500).json({ error: 'Server configuration error: API key not set' });
    return;
  }

  // === Gemini API'ye istek ilet (şeffaf proxy) ===
  try {
    const geminiUrl = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent'
      + '?key=' + geminiApiKey;

    const geminiResponse = await fetch(geminiUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(req.body),
    });

    const data = await geminiResponse.json();

    // === Gemini'nin HTTP durum kodunu ve yanıtını olduğu gibi ilet ===
    res.status(geminiResponse.status).json(data);
  } catch (error) {
    functions.logger.error('Gemini API proxy hatasi:', error.message);
    res.status(502).json({ error: 'AI service temporarily unavailable' });
  }
});
