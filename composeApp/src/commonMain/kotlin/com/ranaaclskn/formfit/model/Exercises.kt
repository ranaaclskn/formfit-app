package com.ranaaclskn.formfit.model

import com.ranaaclskn.formfit.analysis.CheckPhase
import com.ranaaclskn.formfit.analysis.Direction
import com.ranaaclskn.formfit.analysis.ExerciseRule
import com.ranaaclskn.formfit.analysis.FormCheck
import com.ranaaclskn.formfit.analysis.Joint
import com.ranaaclskn.formfit.analysis.PrimaryMotion
import com.ranaaclskn.formfit.analysis.SideMode
import com.ranaaclskn.formfit.analysis.angle
import com.ranaaclskn.formfit.analysis.distance
import com.ranaaclskn.formfit.analysis.isFrontView
import com.ranaaclskn.formfit.analysis.isVisible
import com.ranaaclskn.formfit.analysis.leftSideMoreVisible
import com.ranaaclskn.formfit.analysis.point
import com.ranaaclskn.formfit.analysis.torsoLean
import com.ranaaclskn.formfit.analysis.torsoLength
import com.ranaaclskn.formfit.demo.DemoAnimation
import com.ranaaclskn.formfit.demo.DemoView
import com.ranaaclskn.formfit.demo.keyframe
import com.ranaaclskn.formfit.model.Lm.LEFT_ANKLE
import com.ranaaclskn.formfit.model.Lm.LEFT_ELBOW
import com.ranaaclskn.formfit.model.Lm.LEFT_HIP
import com.ranaaclskn.formfit.model.Lm.LEFT_KNEE
import com.ranaaclskn.formfit.model.Lm.LEFT_SHOULDER
import com.ranaaclskn.formfit.model.Lm.LEFT_WRIST
import com.ranaaclskn.formfit.model.Lm.RIGHT_ANKLE
import com.ranaaclskn.formfit.model.Lm.RIGHT_ELBOW
import com.ranaaclskn.formfit.model.Lm.RIGHT_HIP
import com.ranaaclskn.formfit.model.Lm.RIGHT_KNEE
import com.ranaaclskn.formfit.model.Lm.RIGHT_SHOULDER
import com.ranaaclskn.formfit.model.Lm.RIGHT_WRIST
import com.ranaaclskn.formfit.demo.Joint as DJ
import kotlin.math.abs

private val KNEE = Joint(LEFT_HIP, LEFT_KNEE, LEFT_ANKLE) to Joint(RIGHT_HIP, RIGHT_KNEE, RIGHT_ANKLE)
private val ELBOW = Joint(LEFT_SHOULDER, LEFT_ELBOW, LEFT_WRIST) to Joint(RIGHT_SHOULDER, RIGHT_ELBOW, RIGHT_WRIST)
private val HIP = Joint(LEFT_SHOULDER, LEFT_HIP, LEFT_KNEE) to Joint(RIGHT_SHOULDER, RIGHT_HIP, RIGHT_KNEE)
/** Kolun gövdeden açılma açısı (kalça-omuz-dirsek). */
private val SHOULDER_ABDUCTION = Joint(LEFT_HIP, LEFT_SHOULDER, LEFT_ELBOW) to Joint(RIGHT_HIP, RIGHT_SHOULDER, RIGHT_ELBOW)

private fun motion(
    joints: Pair<Joint, Joint>,
    direction: Direction,
    sideMode: SideMode,
    enterAt: Float,
    targetAt: Float,
    exitAt: Float,
) = PrimaryMotion(joints.first, joints.second, direction, sideMode, enterAt, targetAt, exitAt)

/** Daha iyi görünen taraftaki açı; görünmüyorsa null. */
private fun com.ranaaclskn.formfit.model.PoseFrame.sideAngle(joints: Pair<Joint, Joint>): Float? {
    val (l, r) = joints
    val useLeft = leftSideMoreVisible(l.a to r.a, l.b to r.b, l.c to r.c)
    val j = if (useLeft) l else r
    return if (isVisible(j.a, j.b, j.c)) angle(j.a, j.b, j.c) else null
}

/** İki taraf da görünüyorsa iki açı. */
private fun com.ranaaclskn.formfit.model.PoseFrame.bothAngles(joints: Pair<Joint, Joint>): Pair<Float, Float>? {
    val (l, r) = joints
    if (!isVisible(l.a, l.b, l.c, r.a, r.b, r.c)) return null
    return angle(l.a, l.b, l.c) to angle(r.a, r.b, r.c)
}

private val torsoVisible = intArrayOf(LEFT_SHOULDER, RIGHT_SHOULDER, LEFT_HIP, RIGHT_HIP)

private fun com.ranaaclskn.formfit.model.PoseFrame.leanOrNull(): Float? =
    if (isVisible(*torsoVisible, min = 0.3f)) torsoLean() else null

val EXERCISES: List<Exercise> = listOf(
    Exercise(
        id = "squat",
        name = "Squat",
        category = Category.LOWER,
        muscles = listOf("Ön bacak (quadriceps)", "Kalça (gluteus)", "Arka bacak", "Core"),
        sets = 3, reps = 12,
        cameraAngle = CameraAngle.SIDE,
        setup = "Telefonu bel hizasında, 2-3 metre uzağa koy. Kameraya yan dön; baştan ayağa tüm vücudun görünmeli.",
        steps = listOf(
            "Ayaklarını omuz genişliğinde aç, ayak uçların hafifçe dışa baksın.",
            "Göğsünü dik tut, karın kaslarını sık.",
            "Kalçanı sandalyeye oturur gibi geriye ve aşağıya doğru indir.",
            "Uyluklar yere paralel olana kadar in.",
            "Topuklarından güç alarak başlangıç pozisyonuna geri kalk.",
        ),
        cautions = listOf(
            "Dizlerin içe kapanmasın; dizler ayak uçlarıyla aynı yöne baksın.",
            "Topuklarını yerden kaldırma.",
            "Gövdeni fazla öne eğme, sırtını yuvarlama.",
            "Yarım squat yapma; tam derinliğe in.",
        ),
        tips = listOf(
            "Kollarını öne uzatmak dengeyi kolaylaştırır.",
            "İnerken nefes al, kalkarken nefes ver.",
            "Ayakla yeri 'yırtmaya' çalışır gibi dışa doğru it; dizler kendiliğinden doğru hizaya gelir.",
            "Aşağıda 1 saniye beklemek kontrolü artırır.",
        ),
        rule = ExerciseRule(
            primary = motion(KNEE, Direction.DECREASING, SideMode.MIN, enterAt = 150f, targetAt = 100f, exitAt = 160f),
            depthMessage = "Yeterince inmedin, uyluklar yere paralel olsun",
            checks = listOf(
                FormCheck("Göğsünü dik tut, fazla öne eğiliyorsun") { f, _ -> f.leanOrNull()?.let { it <= 55f } },
                FormCheck("Dizlerin içe kapanıyor, dışa doğru it", phase = CheckPhase.PEAK) { f, _ ->
                    if (!f.isFrontView() || !f.isVisible(LEFT_KNEE, RIGHT_KNEE, LEFT_ANKLE, RIGHT_ANKLE)) null
                    else distance(f.point(LEFT_KNEE), f.point(RIGHT_KNEE)) >=
                        distance(f.point(LEFT_ANKLE), f.point(RIGHT_ANKLE)) * 0.75f
                },
            ),
            minRepMs = 900,
        ),
        demo = DemoAnimation(
            view = DemoView.SIDE,
            start = keyframe(DemoView.SIDE) { },
            end = keyframe(DemoView.SIDE) {
                spine = -55f; upperArm = 0f; forearm = 0f; thigh = 8f; shin = 115f
            },
            anchor = DJ.AN_L,
            toEndCue = "Kalçanı geriye it, aşağı in",
            toStartCue = "Topuklardan it, kalk",
        ),
    ),
    Exercise(
        id = "pushup",
        name = "Şınav",
        category = Category.UPPER,
        muscles = listOf("Göğüs", "Arka kol (triceps)", "Ön omuz", "Core"),
        sets = 3, reps = 10,
        cameraAngle = CameraAngle.SIDE,
        setup = "Telefonu yere, 1.5-2 metre uzağa koy. Kameraya yan profilden görün; baş ve ayakların kadrajda olsun.",
        steps = listOf(
            "Ellerini omuz genişliğinden biraz geniş, omuzlarının altına yerleştir.",
            "Bacaklarını geriye uzat; baştan topuğa düz bir çizgi oluştur.",
            "Dirseklerini bükerek göğsünü yere yaklaştır.",
            "Göğsün yere yaklaşınca avuçlarından iterek yukarı çık.",
        ),
        cautions = listOf(
            "Kalçanın aşağı düşmesine ya da yukarı kalkmasına izin verme.",
            "Dirseklerini 90 derece yana açma; gövdeye yaklaşık 45 derece olsun.",
            "Başını aşağı sarkıtma, bakışın elinin biraz önünde olsun.",
        ),
        tips = listOf(
            "Zorlanıyorsan dizlerin yerde başla, form aynı kalsın.",
            "Karın ve kalça kaslarını sıkmak vücudu düz tutmayı kolaylaştırır.",
            "İnişi 2 saniyede yapmak gücü hızlı artırır.",
        ),
        rule = ExerciseRule(
            primary = motion(ELBOW, Direction.DECREASING, SideMode.MIN, enterAt = 145f, targetAt = 100f, exitAt = 155f),
            depthMessage = "Daha aşağı in, göğsünü yere yaklaştır",
            gate = { f -> f.leanOrNull()?.let { it > 50f } ?: false },
            gateMessage = "Şınav pozisyonuna geç, yandan görün",
            checks = listOf(
                FormCheck("Vücudunu düz tut, kalçan düşmesin/kalkmasın") { f, _ ->
                    val useLeft = f.leftSideMoreVisible(LEFT_SHOULDER to RIGHT_SHOULDER, LEFT_HIP to RIGHT_HIP, LEFT_ANKLE to RIGHT_ANKLE)
                    val (s, h, a) = if (useLeft) Triple(LEFT_SHOULDER, LEFT_HIP, LEFT_ANKLE) else Triple(RIGHT_SHOULDER, RIGHT_HIP, RIGHT_ANKLE)
                    if (!f.isVisible(s, h, a)) null else f.angle(s, h, a) >= 155f
                },
            ),
            minRepMs = 800,
        ),
        demo = DemoAnimation(
            view = DemoView.SIDE,
            start = keyframe(DemoView.SIDE) { spine = -20f; thigh = 160f; shin = 160f; foot = 120f },
            end = keyframe(DemoView.SIDE) {
                spine = -3f; thigh = 177f; shin = 177f; foot = 120f; upperArm = 165f; forearm = 15f
            },
            anchor = DJ.TOE_L,
            toEndCue = "Göğsünü yere yaklaştır",
            toStartCue = "Avuçlarından it",
        ),
    ),
    Exercise(
        id = "lunge",
        name = "Lunge (Hamle)",
        category = Category.LOWER,
        muscles = listOf("Ön bacak", "Kalça", "Arka bacak", "Denge kasları"),
        sets = 3, reps = 10,
        cameraAngle = CameraAngle.SIDE,
        setup = "Telefonu bel hizasında, 2-3 metre uzağa koy ve kameraya yan dön.",
        steps = listOf(
            "Bir ayağınla öne, diğeriyle geriye geniş bir adım al.",
            "Gövdeni dik tutarak iki dizini de bük.",
            "Arka dizin yere değmeden hemen önce dur; ön diz yaklaşık 90 derece olsun.",
            "Ön topuğundan iterek yukarı çık. Bacakları sırayla değiştirebilirsin.",
        ),
        cautions = listOf(
            "Ön dizin içe kaymasın, ayak ucunla aynı hizada olsun.",
            "Gövdeni öne eğme.",
            "Adımın çok kısa olursa ön diz ayak ucunu fazla geçer.",
        ),
        tips = listOf(
            "Ellerini beline koymak dengeyi kolaylaştırır.",
            "Ağırlığını ön topuğunda hisset.",
            "Arka diz düz aşağı insin, öne değil.",
        ),
        rule = ExerciseRule(
            primary = motion(KNEE, Direction.DECREASING, SideMode.MIN, enterAt = 145f, targetAt = 105f, exitAt = 160f),
            depthMessage = "Daha derine in, ön diz 90 dereceye yaklaşsın",
            checks = listOf(
                FormCheck("Gövdeni dik tut, öne eğilme") { f, _ -> f.leanOrNull()?.let { it <= 30f } },
            ),
            minRepMs = 900,
        ),
        demo = DemoAnimation(
            view = DemoView.SIDE,
            start = keyframe(DemoView.SIDE) {
                thigh = 80f; shin = 95f; thighR = 105f; shinR = 95f; footR = 0f
            },
            end = keyframe(DemoView.SIDE) {
                thigh = 10f; shin = 95f; thighR = 95f; shinR = 188f; footR = 90f
            },
            anchor = DJ.AN_L,
            toEndCue = "İki dizini bük, aşağı in",
            toStartCue = "Ön topuktan it, kalk",
        ),
    ),
    Exercise(
        id = "glute-bridge",
        name = "Kalça Köprüsü",
        category = Category.CORE,
        muscles = listOf("Kalça (gluteus)", "Arka bacak", "Bel kasları", "Core"),
        sets = 3, reps = 15,
        cameraAngle = CameraAngle.SIDE,
        setup = "Telefonu yere, 1.5-2 metre uzağa koy. Sırt üstü yat; kamera seni yandan görsün.",
        steps = listOf(
            "Sırt üstü yat, dizlerini bük, ayaklarını kalçana yakın ve yere düz bas.",
            "Kollarını yanlarında yere koy.",
            "Topuklarından iterek kalçanı yukarı kaldır; omuz-kalça-diz düz bir çizgi olsun.",
            "Yukarıda kalçanı sık, 1 saniye bekle ve yavaşça in.",
        ),
        cautions = listOf(
            "Belini aşırı kavislendirerek fazla yükselme.",
            "Ayakların kalçadan çok uzakta olursa yük arka bacağa kayar.",
            "Dizlerin dışa ya da içe açılmasın.",
        ),
        tips = listOf(
            "Ayak uçlarını hafifçe kaldırıp topuklardan itmek kalçayı daha çok çalıştırır.",
            "Yukarıda kalçayı bilinçli şekilde sık.",
            "Zorlaştırmak için tek bacakla yap.",
        ),
        rule = ExerciseRule(
            primary = motion(HIP, Direction.INCREASING, SideMode.MAX, enterAt = 150f, targetAt = 166f, exitAt = 142f),
            depthMessage = "Kalçanı daha yukarı kaldır, vücudun düz bir çizgi olsun",
            gate = { f -> f.leanOrNull()?.let { it > 55f } ?: false },
            gateMessage = "Sırt üstü yat, yandan görün",
            checks = listOf(
                FormCheck("Ayaklarını kalçana biraz daha yaklaştır", phase = CheckPhase.PEAK) { f, _ ->
                    f.sideAngle(KNEE)?.let { it in 60f..120f }
                },
            ),
            minRepMs = 1000,
        ),
        demo = DemoAnimation(
            view = DemoView.SIDE,
            start = keyframe(DemoView.SIDE) {
                spine = 180f; head = 195f; upperArm = 0f; forearm = 0f; thigh = -70f; shin = 95f; foot = 0f
            },
            end = keyframe(DemoView.SIDE) {
                spine = 160f; head = 195f; upperArm = 0f; forearm = 0f; thigh = -30f; shin = 100f; foot = 0f
            },
            anchor = DJ.NECK,
            toEndCue = "Topuklardan it, kalçanı kaldır",
            toStartCue = "Yavaşça in",
            holdEnd = 0.25f,
        ),
    ),
    Exercise(
        id = "biceps-curl",
        name = "Biceps Curl",
        category = Category.UPPER,
        muscles = listOf("Ön kol (biceps)", "Ön kol kasları"),
        sets = 3, reps = 12,
        cameraAngle = CameraAngle.SIDE,
        setup = "Telefonu göğüs hizasında, 2 metre uzağa koy. Kameraya yan dön, çalışan kolun kameraya yakın olsun.",
        steps = listOf(
            "Ayakta dik dur, dambılları avuçların öne bakacak şekilde tut.",
            "Dirseklerini gövdenin yanında sabit tut.",
            "Sadece ön kolunu bükerek ağırlığı omzuna doğru kaldır.",
            "Yukarıda biceps'ini sık, kolunu yavaşça tam düzleşene kadar indir.",
        ),
        cautions = listOf(
            "Dirseklerin öne kaymasın; omuz devreye girer.",
            "Gövdeni sallayarak hız alma.",
            "Kolu aşağıda tam açmadan yeni tekrara başlama.",
        ),
        tips = listOf(
            "İnişi kalkıştan daha yavaş yap (2-3 saniye).",
            "Bileklerini düz tut, bükme.",
            "Sırtını duvara yaslamak sallanmayı engeller.",
        ),
        rule = ExerciseRule(
            primary = motion(ELBOW, Direction.DECREASING, SideMode.MIN, enterAt = 130f, targetAt = 60f, exitAt = 150f),
            depthMessage = "Ağırlığı omzuna kadar kaldır",
            gate = { f -> f.leanOrNull()?.let { it < 35f } ?: false },
            gateMessage = "Ayakta dik dur",
            checks = listOf(
                FormCheck("Dirseğini gövdene sabitle") { f, _ ->
                    val l = if (f.isVisible(LEFT_SHOULDER, LEFT_ELBOW, LEFT_WRIST)) f.angle(LEFT_SHOULDER, LEFT_ELBOW, LEFT_WRIST) else null
                    val r = if (f.isVisible(RIGHT_SHOULDER, RIGHT_ELBOW, RIGHT_WRIST)) f.angle(RIGHT_SHOULDER, RIGHT_ELBOW, RIGHT_WRIST) else null
                    // Bükülen (açısı küçük olan) kolun üst kolu gövdeye ne kadar yakın?
                    val left = when {
                        l == null && r == null -> return@FormCheck null
                        l == null -> false
                        r == null -> true
                        else -> l <= r
                    }
                    val (h, s, e) = if (left) Triple(LEFT_HIP, LEFT_SHOULDER, LEFT_ELBOW) else Triple(RIGHT_HIP, RIGHT_SHOULDER, RIGHT_ELBOW)
                    if (!f.isVisible(h, s, e)) null else f.angle(h, s, e) <= 35f
                },
                FormCheck("Gövdeni sallama, ağırlığı kolunla kaldır") { f, ctx ->
                    f.leanOrNull()?.let { abs(it - ctx.startTorsoLean) <= 12f }
                },
            ),
            minRepMs = 900,
        ),
        demo = DemoAnimation(
            view = DemoView.SIDE,
            start = keyframe(DemoView.SIDE) { },
            end = keyframe(DemoView.SIDE) { upperArm = 85f; forearm = -75f },
            anchor = DJ.AN_L,
            toEndCue = "Dirsek sabit, yukarı bük",
            toStartCue = "Yavaşça indir",
            dumbbells = true,
        ),
        upperName = "BICEPS CURL",
    ),
    Exercise(
        id = "dumbbell-row",
        name = "Dumbbell Row",
        category = Category.BACK,
        muscles = listOf("Sırt (latissimus)", "Arka omuz", "Ön kol (biceps)", "Romboidler"),
        sets = 3, reps = 10,
        cameraAngle = CameraAngle.SIDE,
        setup = "Telefonu bel hizasında, 2 metre uzağa koy. Kameraya yan dön, çeken kolun kameraya yakın olsun.",
        steps = listOf(
            "Dizlerini hafif bük, kalçadan menteşe yaparak gövdeni yaklaşık 45 derece öne eğ.",
            "Sırtın düz, dambıllar kolların uzanmış halde aşağıda olsun.",
            "Dirseğini geriye ve yukarı, kalçana doğru çek.",
            "Yukarıda kürek kemiklerini sık, yavaşça kolunu uzatarak indir.",
        ),
        cautions = listOf(
            "Sırtını yuvarlama; göğüs açık, bel düz kalsın.",
            "Gövdeni kaldırıp indirerek hız alma.",
            "Omuzlarını kulaklarına doğru kaldırma.",
        ),
        tips = listOf(
            "Ağırlığı elinle değil dirseğinle çektiğini düşün.",
            "Dirseği gövdeye yakın tutmak sırtı daha çok çalıştırır.",
            "Boşta kalan elini dizine ya da sehpaya dayamak beli rahatlatır.",
        ),
        rule = ExerciseRule(
            primary = motion(ELBOW, Direction.DECREASING, SideMode.MIN, enterAt = 140f, targetAt = 95f, exitAt = 155f),
            depthMessage = "Dirseğini daha yukarı, kalçana kadar çek",
            gate = { f -> f.leanOrNull()?.let { it in 25f..80f } ?: false },
            gateMessage = "Gövdeni öne eğ, yandan görün",
            checks = listOf(
                FormCheck("Gövdeni sabit tut, kalkıp inme") { f, ctx ->
                    f.leanOrNull()?.let { abs(it - ctx.startTorsoLean) <= 12f }
                },
            ),
            minRepMs = 900,
        ),
        demo = DemoAnimation(
            view = DemoView.SIDE,
            start = keyframe(DemoView.SIDE) { spine = -40f; thigh = 80f; shin = 100f },
            end = keyframe(DemoView.SIDE) { spine = -40f; thigh = 80f; shin = 100f; upperArm = 160f; forearm = 95f },
            anchor = DJ.AN_L,
            toEndCue = "Dirseği kalçaya çek",
            toStartCue = "Yavaşça uzat",
            dumbbells = true,
        ),
        upperName = "DUMBBELL ROW",
    ),
    Exercise(
        id = "romanian-deadlift",
        name = "Romanian Deadlift",
        category = Category.BACK,
        muscles = listOf("Arka bacak (hamstring)", "Kalça", "Bel kasları", "Sırt"),
        sets = 3, reps = 10,
        cameraAngle = CameraAngle.SIDE,
        setup = "Telefonu bel hizasında, 2-3 metre uzağa koy ve kameraya yan dön.",
        steps = listOf(
            "Ayakların kalça genişliğinde, dambıllar uyluklarının önünde dik dur.",
            "Dizlerini hafifçe bük ve bu açıyı sabit tut.",
            "Kalçanı geriye iterek gövdeni öne eğ; ağırlıklar bacaklarına yakın insin.",
            "Arka bacağında gerilme hissedince kalçanı öne iterek dik konuma dön.",
        ),
        cautions = listOf(
            "Sırtını yuvarlama, bel her zaman düz kalsın.",
            "Squat'a çevirme: dizler fazla bükülmesin, hareket kalçadan.",
            "Ağırlıkları bacaklardan uzaklaştırma.",
        ),
        tips = listOf(
            "Kalçanla arkandaki duvara dokunmaya çalıştığını düşün.",
            "İnişi 2-3 saniyede yapmak arka bacağı daha çok çalıştırır.",
            "Yukarıda kalçanı sık ama geriye yaslanma.",
        ),
        rule = ExerciseRule(
            primary = motion(HIP, Direction.DECREASING, SideMode.MIN, enterAt = 150f, targetAt = 115f, exitAt = 162f),
            depthMessage = "Kalçanı daha geriye it, gövden daha çok eğilsin",
            checks = listOf(
                FormCheck("Dizlerini fazla bükme, hareket kalçadan olsun") { f, _ ->
                    f.sideAngle(KNEE)?.let { it >= 135f }
                },
            ),
            minRepMs = 1000,
        ),
        demo = DemoAnimation(
            view = DemoView.SIDE,
            start = keyframe(DemoView.SIDE) { },
            end = keyframe(DemoView.SIDE) { spine = -12f; thigh = 78f; shin = 95f },
            anchor = DJ.AN_L,
            toEndCue = "Kalçayı geri it, eğil",
            toStartCue = "Kalçayı öne it, kalk",
            dumbbells = true,
        ),
        upperName = "ROMANIAN DEADLIFT",
    ),
    Exercise(
        id = "shoulder-press",
        name = "Omuz Press",
        category = Category.UPPER,
        muscles = listOf("Omuz (deltoid)", "Arka kol (triceps)", "Üst sırt"),
        sets = 3, reps = 10,
        cameraAngle = CameraAngle.FRONT,
        setup = "Telefonu göğüs hizasında, 2-3 metre uzağa koy. Kameraya önden bak; kolların yukarıda da kadrajda kalsın.",
        steps = listOf(
            "Dambılları kulak hizasında, dirsekler aşağı bakacak şekilde tut.",
            "Karın kaslarını sık, belini düz tut.",
            "Ağırlıkları başının üzerine, kolların neredeyse düzleşene kadar it.",
            "Kontrollü şekilde kulak hizasına geri indir.",
        ),
        cautions = listOf(
            "Belini geriye kavislendirme.",
            "Ağırlıkları kulak hizasının altına düşürme, omuz gerginliği yaratır.",
            "İki kolu farklı hızda kaldırma.",
        ),
        tips = listOf(
            "Kalçanı ve karnını sıkmak beli korur.",
            "Yukarıda dirsekleri tam kilitlemeden dur.",
            "Oturarak yapmak gövde sallanmasını azaltır.",
        ),
        rule = ExerciseRule(
            primary = motion(ELBOW, Direction.INCREASING, SideMode.AVERAGE, enterAt = 120f, targetAt = 155f, exitAt = 105f),
            depthMessage = "Kollarını yukarıda tam uzat",
            gate = { f ->
                val l = f.isVisible(LEFT_WRIST, LEFT_SHOULDER) && f[LEFT_WRIST].y < f[LEFT_SHOULDER].y
                val r = f.isVisible(RIGHT_WRIST, RIGHT_SHOULDER) && f[RIGHT_WRIST].y < f[RIGHT_SHOULDER].y
                l || r
            },
            gateMessage = "Ağırlıkları omuz hizasına kaldır",
            checks = listOf(
                FormCheck("İki kolu eşit kaldır") { f, _ ->
                    if (!f.isVisible(LEFT_WRIST, RIGHT_WRIST)) null
                    else abs(f.point(LEFT_WRIST).y - f.point(RIGHT_WRIST).y) <= f.torsoLength() * 0.2f
                },
                FormCheck("Belini kavislendirme, gövden dik kalsın") { f, _ -> f.leanOrNull()?.let { it <= 15f } },
            ),
            minRepMs = 900,
        ),
        demo = DemoAnimation(
            view = DemoView.FRONT,
            start = keyframe(DemoView.FRONT) { upperArm = 165f; forearm = -90f; thigh = 95f; shin = 90f },
            end = keyframe(DemoView.FRONT) { upperArm = -105f; forearm = -95f; thigh = 95f; shin = 90f },
            anchor = DJ.HIP_C,
            toEndCue = "Yukarı it",
            toStartCue = "Kulak hizasına indir",
            dumbbells = true,
        ),
    ),
    Exercise(
        id = "lateral-raise",
        name = "Yana Açış (Lateral Raise)",
        category = Category.UPPER,
        muscles = listOf("Yan omuz (deltoid)", "Trapez"),
        sets = 3, reps = 12,
        cameraAngle = CameraAngle.FRONT,
        setup = "Telefonu göğüs hizasında, 2-3 metre uzağa koy ve kameraya önden bak.",
        steps = listOf(
            "Dambılları yanlarında tut, dirseklerin hafif bükük olsun.",
            "Kollarını yanlara doğru omuz hizasına kadar kaldır.",
            "Yukarıda kısa bir an dur.",
            "Yavaşça yanlarına indir.",
        ),
        cautions = listOf(
            "Kolları omuz hizasından yukarı kaldırma.",
            "Gövdeni sallayarak hız alma.",
            "Omuzlarını kulaklarına doğru kaldırma.",
        ),
        tips = listOf(
            "Elinde sürahi varmış da su döküyormuşsun gibi serçe parmağını hafif yukarı çevir.",
            "Hafif ağırlıkla, yavaş tempoda yapmak daha etkilidir.",
            "Dirseği elden önde tutmayı düşün.",
        ),
        rule = ExerciseRule(
            primary = motion(SHOULDER_ABDUCTION, Direction.INCREASING, SideMode.AVERAGE, enterAt = 35f, targetAt = 75f, exitAt = 25f),
            depthMessage = "Kollarını omuz hizasına kadar kaldır",
            checks = listOf(
                FormCheck("Kolları omuz hizasından yukarı kaldırma", minFailRatio = 0.2f) { f, _ ->
                    f.sideAngle(SHOULDER_ABDUCTION)?.let { it <= 105f }
                },
                FormCheck("Dirseklerin hafif bükük ama sabit kalsın") { f, _ -> f.sideAngle(ELBOW)?.let { it >= 130f } },
                FormCheck("İki kolu aynı anda kaldır") { f, _ ->
                    f.bothAngles(SHOULDER_ABDUCTION)?.let { (l, r) -> abs(l - r) <= 25f }
                },
                FormCheck("Gövdeni sallama") { f, ctx -> f.leanOrNull()?.let { abs(it - ctx.startTorsoLean) <= 10f } },
            ),
            minRepMs = 1000,
        ),
        demo = DemoAnimation(
            view = DemoView.FRONT,
            start = keyframe(DemoView.FRONT) { upperArm = 100f; forearm = 98f; thigh = 95f; shin = 90f },
            end = keyframe(DemoView.FRONT) { upperArm = 175f; forearm = 172f; thigh = 95f; shin = 90f },
            anchor = DJ.HIP_C,
            toEndCue = "Yanlara, omuz hizasına kaldır",
            toStartCue = "Yavaşça indir",
            dumbbells = true,
        ),
        upperName = "YANA AÇIŞ (LATERAL RAISE)",
    ),
    Exercise(
        id = "jumping-jack",
        name = "Jumping Jack",
        category = Category.CARDIO,
        muscles = listOf("Tüm vücut", "Baldır", "Omuz", "Kalp-damar"),
        sets = 3, reps = 20,
        cameraAngle = CameraAngle.FRONT,
        setup = "Telefonu bel hizasında, 3 metre uzağa koy. Kameraya önden bak; kolların yukarıdayken de kadrajda ol.",
        steps = listOf(
            "Ayakların bitişik, kolların yanında dik dur.",
            "Zıplayarak bacaklarını yana aç, aynı anda kollarını başının üstüne kaldır.",
            "Tekrar zıplayarak başlangıç pozisyonuna dön.",
            "Ritmi koruyarak devam et.",
        ),
        cautions = listOf(
            "Ayak uçlarına yumuşak bas, topukla sert inme.",
            "Dizlerini kilitleme, hafif bükük tut.",
            "Kolları yarıda bırakma, başının üstüne kadar kaldır.",
        ),
        tips = listOf(
            "Eklemlerin hassassa zıplamadan, bacakları sırayla yana açarak yap.",
            "Nefesini ritme göre düzenle.",
            "Isınma hareketi olarak 30-60 saniye idealdir.",
        ),
        rule = ExerciseRule(
            primary = motion(SHOULDER_ABDUCTION, Direction.INCREASING, SideMode.AVERAGE, enterAt = 70f, targetAt = 140f, exitAt = 45f),
            depthMessage = "Kollarını başının üstüne kadar kaldır",
            checks = listOf(
                FormCheck("Kolları kaldırırken bacaklarını da aç", phase = CheckPhase.PEAK) { f, _ ->
                    if (!f.isVisible(LEFT_ANKLE, RIGHT_ANKLE, LEFT_SHOULDER, RIGHT_SHOULDER)) null
                    else distance(f.point(LEFT_ANKLE), f.point(RIGHT_ANKLE)) >=
                        distance(f.point(LEFT_SHOULDER), f.point(RIGHT_SHOULDER)) * 1.1f
                },
            ),
            minRepMs = 350,
        ),
        demo = DemoAnimation(
            view = DemoView.FRONT,
            start = keyframe(DemoView.FRONT) { upperArm = 98f; forearm = 95f; thigh = 92f; shin = 90f },
            end = keyframe(DemoView.FRONT) { upperArm = -130f; forearm = -110f; thigh = 112f; shin = 108f },
            anchor = DJ.HIP_C,
            toEndCue = "Zıpla, kolları ve bacakları aç",
            toStartCue = "Zıpla, kapat",
            cycleMs = 1200,
            holdStart = 0.05f,
            holdEnd = 0.05f,
        ),
        upperName = "JUMPING JACK",
    ),
).sortedBy { it.category.ordinal } // Kategoriler arka arkaya gelsin, numaralar sıralı olsun

fun exerciseById(id: String): Exercise? = EXERCISES.firstOrNull { it.id == id }
