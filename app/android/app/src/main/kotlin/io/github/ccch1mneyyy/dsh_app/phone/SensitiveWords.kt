package io.github.ccch1mneyyy.dsh_app.phone

// The word lists behind the safety rules, in one place so the
// core's SafetyGate (which asks) and the accessibility ScreenReader (which marks
// `⚠` on the lines the gate will ask about) can never drift apart. Plain Kotlin,
// no Android types: JVM-tested.

object SensitiveWords {
    /** Sensitive actions: a target whose label or view id hits one of these needs a confirm. */
    val confirm: List<String> = listOf(
        // l10n:keep external UI matcher
        "发送", "发表", "回复", "转发", "发布", "上传", "群发", "分享",
        // l10n:keep external UI matcher
        "加好友", "添加好友", "添加到通讯录", "关注", "点赞", "红包",
        // l10n:keep external UI matcher
        "保存", "提交", "删除", "移除", "清空", "卸载", "注销", "退出登录", "退出账号",
        // l10n:keep external UI matcher
        "支付", "付款", "转账", "购买", "下单", "结算", "充值", "提现", "退款", "订阅", "续费", "买入", "卖出", "撤单",
        // l10n:keep external UI matcher
        "登录", "修改密码", "重置密码", "绑定", "解绑", "授权", "同意", "安装", "拨打", "呼叫",
    )

    /** The same intent in view-id words (`btn_send`, `payButton`). Matched as whole id words. */
    val confirmIdWords: Set<String> = setOf(
        "send", "reply", "forward", "publish", "upload", "share", "follow", "like", "addfriend",
        "save", "submit", "delete", "remove", "uninstall", "logout", "signout", "unregister",
        "pay", "payment", "checkout", "purchase", "buy", "transfer", "recharge", "topup",
        "withdraw", "refund", "subscribe", "renew", "redpacket", "hongbao",
        "login", "signin", "changepassword", "resetpassword", "bind", "unbind", "authorize", "agree", "install", "dial",
    )

    /** English control labels use word boundaries: "buy" must not match "buyer". */
    private val confirmEnglish = ((confirmIdWords - setOf("like")) + setOf(
        "post", "clear", "call", "log in", "sign in", "log out", "sign out", "place order",
        "red packet", "top up", "add friend", "change password", "reset password",
    )).map { word -> word to wholeWords(word) }

    private fun wholeWords(word: String): Regex = Regex(
        "(?<![\\p{L}\\p{N}_])" + word.split(' ').joinToString("\\s+") { Regex.escape(it) } +
            "(?![\\p{L}\\p{N}_])", RegexOption.IGNORE_CASE,
    )

    private val otpHint = wholeWords("otp")
    private val oneTimeHint = wholeWords("one-time")

    /** Secret inputs: their contents stay masked even when the person permits typing. */
    val secret: List<String> = listOf(
        // l10n:keep external UI matcher
        "支付密码", "密码", "验证码", "校验码", "动态码", "安全码", "cvv", "cvc",
        "password", "passcode", "verification code",
    )

    /** Secret-field view-id words (`et_pwd`, `smsCode`). */
    val secretIdWords: Set<String> = setOf(
        "password", "passwd", "pwd", "passcode", "otp", "cvv", "cvc", "smscode", "verifycode", "authcode",
    )

    /**
     * Labels longer than this are content (a chat line, a card), not a control: they only
     * count when the word opens or closes the label. Keeps `⚠` off every message that
     * happens to contain "发送".
     */
    const val SHORT_LABEL = 24

    /**
     * The word that makes tapping this target sensitive, or null. [label] is the rendered
     * text (text → contentDescription → hint), [viewId] the resource id if any.
     */
    fun confirmHit(label: String?, viewId: String?): String? {
        val text = label?.trim().orEmpty()
        if (text.isNotEmpty()) {
            for (word in confirm) {
                if (!text.contains(word)) continue
                if (text.length <= SHORT_LABEL || text.startsWith(word) || text.endsWith(word)) return word
            }
            if (text.equals("like", ignoreCase = true)) return "like"
            for ((word, pattern) in confirmEnglish) {
                for (match in pattern.findAll(text)) {
                    if (text.length <= SHORT_LABEL || match.range.first == 0 || match.range.last == text.lastIndex)
                        return word
                }
            }
        }
        return idWords(viewId).firstOrNull { it in confirmIdWords }
    }

    /** The word that makes this input a secret (password / code) field, or null. */
    fun secretHit(hint: String?, description: String?, viewId: String?): String? {
        for (value in listOf(hint, description)) {
            val text = value?.lowercase() ?: continue
            secret.firstOrNull { text.contains(it) }?.let { return it }
            // Keep the background reader's OTP / one-time hints without matching "hotpot".
            if (otpHint.containsMatchIn(text)) return "otp"
            if (oneTimeHint.containsMatchIn(text)) return "one-time"
        }
        return idWords(viewId).firstOrNull { it in secretIdWords }
    }

    /**
     * `com.tencent.mm:id/btn_sendMsg` → [btn, send, msg, btnsend, sendmsg]: the id tail split
     * on separators and camelCase, lower-cased, plus each pair of neighbours joined (so
     * `verify_code` also yields `verifycode`).
     */
    fun idWords(viewId: String?): List<String> {
        if (viewId.isNullOrBlank()) return emptyList()
        val tail = viewId.substringAfterLast(":id/").substringAfterLast('/')
        val words = mutableListOf<String>()
        val current = StringBuilder()
        fun flush() { if (current.isNotEmpty()) { words.add(current.toString().lowercase()); current.setLength(0) } }
        for ((index, char) in tail.withIndex()) {
            when {
                !char.isLetterOrDigit() -> flush()
                char.isUpperCase() && index > 0 && tail[index - 1].isLowerCase() -> { flush(); current.append(char) }
                else -> current.append(char)
            }
        }
        flush()
        return words + words.zipWithNext { a, b -> a + b }
    }
}
