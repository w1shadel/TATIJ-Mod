package com.maxwell.translate_all_text_into_japanese.processor.lang;

import com.maxwell.translate_all_text_into_japanese.util.AutoTransLog;
import com.maxwell.translate_all_text_into_japanese.processor.GoogleTranslator;
import com.maxwell.translate_all_text_into_japanese.processor.TranslationCache;

import java.util.concurrent.*;

public class TranslationDispatcher {
    private static final BlockingQueue<String> URGENT = new LinkedBlockingQueue<>();
    private static final BlockingQueue<String> BACKGROUND = new LinkedBlockingQueue<>();
    private static final java.util.concurrent.atomic.AtomicInteger ACTIVE_TASKS = new java.util.concurrent.atomic.AtomicInteger(0);
    // Google翻訳APIへのアクセス過多による429（一時遮断）を防ぐため、スレッド数を2に制限
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);



    static {
        for (int i = 0; i < 2; i++) {
            EXECUTOR.submit(TranslationDispatcher::processLoop);
        }
    }

    public static void submit(String text, boolean urgent) {
        if (text == null || text.trim().isEmpty()) return;
        if (isAlreadyTranslated(text)) return;

        if (urgent) {
            if (!URGENT.contains(text)) URGENT.offer(text);
        } else {
            if (!BACKGROUND.contains(text)) BACKGROUND.offer(text);
        }
    }

    private static boolean isAlreadyTranslated(String text) {
        // 翻訳結果が原文と同じ英単語（固有名詞等）だった場合も、処理完了とみなす
        return TranslationCache.contains(text);
    }

    private static void processLoop() {
        while (true) {
            try {
                java.util.List<String> batch = new java.util.ArrayList<>();
                int currentLength = 0;
                
                while (batch.size() < 20 && currentLength < 1500) {
                    String p = URGENT.poll();
                    if (p == null) {
                        if (batch.isEmpty()) {
                            // バッチが空なら最大500ms待機
                            p = BACKGROUND.poll(500, TimeUnit.MILLISECONDS);
                        } else {
                            // 既に要素があれば非ブロッキングで取得
                            p = BACKGROUND.poll();
                        }
                    }
                    if (p != null) {
                        batch.add(p);
                        currentLength += p.length();
                    } else {
                        break;
                    }
                }

                if (batch.isEmpty()) continue;

                ACTIVE_TASKS.addAndGet(batch.size());
                try {
                    StringBuilder requestText = new StringBuilder();
                    for (int i = 0; i < batch.size(); i++) {
                        requestText.append(batch.get(i));
                        if (i < batch.size() - 1) {
                            requestText.append("\n\n<B_SEP>\n\n");
                        }
                    }

                    // タグ保護（<B_SEP> も保護対象になり翻訳の破壊を防ぐ）
                    TagProtector.ProtectionResult protection = TagProtector.protect(requestText.toString());
                    String translated = GoogleTranslator.fetchTranslation(protection.protectedText);

                    if (translated == null) {
                        AutoTransLog.LOGGER.info("バッチ翻訳失敗（{}件）。リトライキューに追加します。", batch.size());
                        batch.forEach(BACKGROUND::offer);
                        Thread.sleep(5000); // 連続失敗防止のウェイトを長めに
                        continue;
                    }

                    if (translated != null && !translated.trim().isEmpty()) {
                        String finalResult = TagProtector.restore(translated, protection.tags);
                        String[] results = finalResult.split("\\s*<B_SEP>\\s*");

                        if (results.length == batch.size()) {
                            for (int i = 0; i < batch.size(); i++) {
                                String orig = batch.get(i);
                                String trans = results[i].trim();
                                if (!trans.isEmpty()) {
                                    TranslationCache.put(orig, trans);
                                }
                            }
                            TranslationCache.save();

                            int remaining = URGENT.size() + BACKGROUND.size() + ACTIVE_TASKS.get();
                            if (remaining % 20 < batch.size() && remaining > 0) {
                                AutoTransLog.LOGGER.info("バッチ翻訳完了（{}件）。残りキュー: {} 件", batch.size(), remaining);
                            }
                        } else {
                            // ごく稀にGoogleが区切り文字を混同した場合のフォールバック
                            AutoTransLog.LOGGER.warn("バッチ分割不一致 (期待:{}, 実際:{})。個別リトライへ移行します。", batch.size(), results.length);
                            for (String orig : batch) {
                                TagProtector.ProtectionResult singleProt = TagProtector.protect(orig);
                                String singleTrans = GoogleTranslator.fetchTranslation(singleProt.protectedText);
                                if (singleTrans != null && !singleTrans.trim().isEmpty()) {
                                    String fr = TagProtector.restore(singleTrans, singleProt.tags);
                                    TranslationCache.put(orig, fr);
                                }
                                Thread.sleep(1500); // APIアクセス制限対策
                            }
                            TranslationCache.save();
                        }
                    }
                    
                    // 次のリクエストまで少し待機（API負荷軽減）
                    Thread.sleep(1500);
                } finally {
                    ACTIVE_TASKS.addAndGet(-batch.size());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                AutoTransLog.LOGGER.error("Dispatcher loop error: {}", e.getMessage());
            }
        }
    }
}