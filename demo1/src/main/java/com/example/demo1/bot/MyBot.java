package com.example.demo1.bot;

import com.example.demo1.entity.Role;
import com.example.demo1.entity.User;
import com.example.demo1.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Component
public class MyBot extends TelegramLongPollingBot {

    private final String botToken;
    private UserRepository userRepository;

    private final Long adminChatId = 6796801984L;

    private boolean waitingForBroadcast = false;

    public MyBot(String botToken, UserRepository userRepository) {
        this.botToken = botToken;
        this.userRepository = userRepository;
    }

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage()) return;

        Long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();
        String username = update.getMessage().getFrom().getUserName();

        if (text != null && text.equalsIgnoreCase("/start")) {
            SendMessage sm = new SendMessage(chatId.toString(),
                    "👋 Assalomu alaykum <b><i>" + (username != null ? username : "mehmon") + "</i></b>!\n\n" +
                            "💱 Bu bot orqali siz <b>Markaziy Bankning rasmiy valyuta kurslarini</b> bilib olishingiz mumkin.\n\n" +
                            "📱 Iltimos, avval raqamingizni ulashing:");
            sm.setParseMode("HTML");

            ReplyKeyboardMarkup contactKb = new ReplyKeyboardMarkup();
            contactKb.setResizeKeyboard(true);
            contactKb.setOneTimeKeyboard(true);

            List<KeyboardRow> kblist = new ArrayList<>();
            KeyboardRow r = new KeyboardRow();
            KeyboardButton b = new KeyboardButton("📱 Raqamni ulashish");
            b.setRequestContact(true);
            r.add(b);
            kblist.add(r);

            contactKb.setKeyboard(kblist);
            sm.setReplyMarkup(contactKb);
            execute(sm);
            return;
        }

        if (update.getMessage().hasContact()) {
            String phone = update.getMessage().getContact().getPhoneNumber();
            String tgUsername = update.getMessage().getFrom().getUserName();

            userRepository.findByChatId(chatId).ifPresentOrElse(u -> {
                u.setPhone(phone);
                u.setUsername(tgUsername != null ? tgUsername : "no_username");
                u.setRole(isAdmin(chatId) ? Role.ADMIN : Role.USER);
                userRepository.save(u);
            }, () -> {
                User u = User.builder()
                        .chatId(chatId)
                        .username(tgUsername != null ? tgUsername : "no_username")
                        .phone(phone)
                        .role(isAdmin(chatId) ? Role.ADMIN : Role.USER)
                        .build();
                userRepository.save(u);
            });

            SendMessage sm = new SendMessage(chatId.toString(), "✅ Raqamingiz qabul qilindi!\n📞 " + phone);
            sm.setReplyMarkup(getMainMenu(chatId));
            execute(sm);
            return;
        }

        if (text == null) return;

        if (isAdmin(chatId) && waitingForBroadcast && !text.equals("📢 Ommaviy xabar")) {
            int success = 0;
            List<User> all = userRepository.findAll();
            for (User u : all) {
                if (u.getChatId() == null) continue;
                try {
                    SendMessage b = new SendMessage(u.getChatId().toString(), text);
                    execute(b);
                    success++;
                } catch (Exception ignored) {}
            }
            waitingForBroadcast = false;

            SendMessage sm = new SendMessage(chatId.toString(),
                    "✅ Xabar <b>" + success + "</b> ta foydalanuvchiga yuborildi!");
            sm.setParseMode("HTML");
            sm.setReplyMarkup(getMainMenu(chatId));
            execute(sm);
            return;
        }

        SendMessage sm = new SendMessage();
        sm.setChatId(chatId.toString());

        switch (text) {
            case "📚 Valyuta kurslari":
                sm.setText(getCurrencyRates());
                sm.setParseMode("HTML");
                break;

            case "ℹ️ Biz haqimizda":
                sm.setText("ℹ️ <b>Biz haqimizda</b>\n\n" +
                        "👋 Ushbu bot orqali siz <b>Markaziy Bankning rasmiy valyuta kurslarini</b> bilib olasiz.\n\n" +
                        "✅ Kurslar har kuni yangilanadi.\n" +
                        "✅ USD, EUR, RUB, GBP va boshqa ko‘plab valyutalar mavjud.\n\n" +
                        "💡 Maqsadimiz – sizga ishonchli ma’lumotni tezkor yetkazish.");
                sm.setParseMode("HTML");
                break;

            case "📞 Aloqa":
                sm.setText("📞 <b>Aloqa ma’lumotlari</b>\n\n" +
                        "☎️ Telefon: +998 90 123 45 67\n" +
                        "📧 Email: info@kurslar.uz\n" +
                        "🌐 Web: https://kurslar.uz\n\n" +
                        "📍 Manzil: Toshkent shahri, Yunusobod tumani");
                sm.setParseMode("HTML");
                break;

            case "💻 Developer":
                sm.setText("💻 Assalomu alaykum!\n\n" +
                        "Menga quyidagi username orqali murojaat qiling 👇\n" +
                        "👉 @amonovwx");
                break;

            case "📢 Ommaviy xabar":
                if (isAdmin(chatId)) {
                    waitingForBroadcast = true;
                    sm.setText("📤 Yubormoqchi bo‘lgan xabaringizni kiriting.\n" +
                            "❌ Bekor qilish uchun: /cancel");
                } else {
                    sm.setText("❌ Siz admin emassiz.");
                }
                break;

            case "/cancel":
                waitingForBroadcast = false;
                sm.setText("↩️ Broadcast bekor qilindi.");
                break;

            default:
                sm.setText("❌ Noma’lum buyruq.");
        }

        sm.setReplyMarkup(getMainMenu(chatId));
        execute(sm);
    }

    private boolean isAdmin(Long chatId) {
        return chatId != null && chatId.equals(adminChatId);
    }

    private ReplyKeyboardMarkup getMainMenu(Long chatId) {
        ReplyKeyboardMarkup kb = new ReplyKeyboardMarkup();
        kb.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow r1 = new KeyboardRow();
        r1.add(new KeyboardButton("📚 Valyuta kurslari"));
        r1.add(new KeyboardButton("ℹ️ Biz haqimizda"));
        rows.add(r1);

        KeyboardRow r2 = new KeyboardRow();
        r2.add(new KeyboardButton("📞 Aloqa"));
        r2.add(new KeyboardButton("💻 Developer"));
        rows.add(r2);

        if (isAdmin(chatId)) {
            KeyboardRow r3 = new KeyboardRow();
            r3.add(new KeyboardButton("📢 Ommaviy xabar"));
            rows.add(r3);
        }

        kb.setKeyboard(rows);
        return kb;
    }

    private String getCurrencyRates() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://cbu.uz/uz/arkhiv-kursov-valyut/json/";
            List<LinkedHashMap<String, Object>> list = restTemplate.getForObject(url, List.class);

            if (list == null || list.isEmpty()) {
                return "❌ Valyuta kurslari mavjud emas!";
            }

            StringBuilder sb = new StringBuilder("💱 <b>Bugungi valyuta kurslari (Markaziy bank):</b>\n\n");
            int count = 1;
            for (LinkedHashMap<String, Object> m : list) {
                String code = (String) m.get("Ccy");
                String name = (String) m.get("CcyNm_UZ");
                String rate = (String) m.get("Rate");
                String diff = (String) m.get("Diff");
                String date = (String) m.get("Date");

                sb.append(count++).append(". <b>").append(code).append("</b> — ").append(name)
                        .append("\n   💵 Kurs: <b>").append(rate).append("</b> so'm")
                        .append("\n   📊 O‘zgarish: ").append(diff)
                        .append("\n   📅 Sana: ").append(date)
                        .append("\n\n");

                if (count > 10) break;
            }
            sb.append("📌 To‘liq ma’lumot: <a href='https://cbu.uz/uz/arkhiv-kursov-valyut/'>CBU sayti</a>");
            return sb.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Valyuta kurslarini olishda xatolik yuz berdi!";
        }
    }

    @Override
    public String getBotUsername() {
        return "KurslarTodayBot";
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}
