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

    // 自動リロード管理用フラグ
    private static volatile boolean wasProcessing = false;
    private static volatile int emptyConsecutiveSeconds = 0;

    static {
        for (int i = 0; i < 2; i++) {
            EXECUTOR.submit(TranslationDispatcher::processLoop);
        }

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(() -> {
            int remaining = URGENT.size() + BACKGROUND.size() + ACTIVE_TASKS.get();
            if (remaining > 0) {
                wasProcessing = true;
                emptyConsecutiveSeconds = 0;
            } else if (wasProcessing) {
                emptyConsecutiveSeconds++;
                if (emptyConsecutiveSeconds >= 5) {
                    wasProcessing = false;
                    emptyConsecutiveSeconds = 0;
                    
                    try {
                        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                        // ユーザーがワールド内にいる場合のみ自動リロードを発火
                        if (mc != null && mc.level != null) {
                            mc.execute(() -> {
                                mc.reloadResourcePacks();
                                if (mc.player != null) {
                                    mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal("§a[AutoTrans] 翻訳処理が完了したため、リソースを自動更新しました。"), false);
                                }
                            });
                        }
                    } catch (Exception e) {
                        AutoTransLog.LOGGER.error("Failed to trigger auto reload: {}", e.getMessage());
                    }
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
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
                String text = URGENT.poll();
                if (text == null) {
                    text = BACKGROUND.poll(500, TimeUnit.MILLISECONDS);
                }

                if (text != null) {
                    ACTIVE_TASKS.incrementAndGet();
                    try {
                        // タグ保護
                        TagProtector.ProtectionResult protection = TagProtector.protect(text);
                        String translated = GoogleTranslator.fetchTranslation(protection.protectedText);

                        if (translated == null) {
                            // 翻訳失敗（レート制限やエラー）の場合は、バックグラウンドキューに戻してリトライ
                            AutoTransLog.LOGGER.info("翻訳失敗のためリトライキューに追加: {}", text);
                            BACKGROUND.offer(text);
                            Thread.sleep(5000); // 連続失敗防止のウェイトを長めに
                            continue;
                        }

                        if (translated != null && !translated.trim().isEmpty()) {
                            String finalResult = TagProtector.restore(translated, protection.tags);
                            TranslationCache.put(text, finalResult);
                            TranslationCache.save();

                            int remaining = URGENT.size() + BACKGROUND.size() + ACTIVE_TASKS.get();
                            if (remaining % 10 == 0 && remaining > 0) {
                                AutoTransLog.LOGGER.info("翻訳完了。残りキュー: {} 件", remaining);
                            }
                        }
                        
                        // 次のリクエストまで少し待機（API負荷軽減）
                        Thread.sleep(1000);
                    } finally {
                        ACTIVE_TASKS.decrementAndGet();
                    }
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