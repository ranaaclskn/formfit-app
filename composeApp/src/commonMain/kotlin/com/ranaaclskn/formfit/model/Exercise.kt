package com.ranaaclskn.formfit.model

import com.ranaaclskn.formfit.analysis.ExerciseRule
import com.ranaaclskn.formfit.demo.DemoAnimation

enum class Category(val title: String) {
    LOWER("Alt Vücut"),
    UPPER("Üst Vücut"),
    BACK("Sırt"),
    CORE("Kalça & Core"),
    CARDIO("Kardiyo"),
}

enum class CameraAngle(val title: String) {
    SIDE("Yandan çek"),
    FRONT("Önden çek"),
}

class Exercise(
    val id: String,
    val name: String,
    val category: Category,
    val muscles: List<String>,
    val sets: Int,
    val reps: Int,
    val cameraAngle: CameraAngle,
    /** Telefonun nereye konacağı. */
    val setup: String,
    /** Nasıl yapılır, adım adım. */
    val steps: List<String>,
    /** Dikkat edilmesi gerekenler / sık yapılan hatalar. */
    val cautions: List<String>,
    /** Püf noktaları. */
    val tips: List<String>,
    val rule: ExerciseRule,
    val demo: DemoAnimation,
    /** Büyük harfli başlık; İngilizce isimlerde Türkçe "İ" kuralı uygulanmasın diye elle verilir. */
    val upperName: String? = null,
)
