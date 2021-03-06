// TODO: muted automatically, investigate should it be ran for JS_IR or not
// IGNORE_BACKEND: JS_IR

// TODO: muted automatically, investigate should it be ran for JVM_IR or not
// IGNORE_BACKEND: JVM_IR

// Auto-generated by org.jetbrains.kotlin.generators.tests.GenerateRangesCodegenTestData. DO NOT EDIT!
// WITH_RUNTIME



fun box(): String {
    val list1 = ArrayList<Int>()
    val range1 = 5..5
    for (i in range1) {
        list1.add(i)
        if (list1.size > 23) break
    }
    if (list1 != listOf<Int>(5)) {
        return "Wrong elements for 5..5: $list1"
    }

    val list2 = ArrayList<Int>()
    val range2 = 5.toByte()..5.toByte()
    for (i in range2) {
        list2.add(i)
        if (list2.size > 23) break
    }
    if (list2 != listOf<Int>(5)) {
        return "Wrong elements for 5.toByte()..5.toByte(): $list2"
    }

    val list3 = ArrayList<Int>()
    val range3 = 5.toShort()..5.toShort()
    for (i in range3) {
        list3.add(i)
        if (list3.size > 23) break
    }
    if (list3 != listOf<Int>(5)) {
        return "Wrong elements for 5.toShort()..5.toShort(): $list3"
    }

    val list4 = ArrayList<Long>()
    val range4 = 5L..5L
    for (i in range4) {
        list4.add(i)
        if (list4.size > 23) break
    }
    if (list4 != listOf<Long>(5L)) {
        return "Wrong elements for 5L..5L: $list4"
    }

    val list5 = ArrayList<Char>()
    val range5 = 'k'..'k'
    for (i in range5) {
        list5.add(i)
        if (list5.size > 23) break
    }
    if (list5 != listOf<Char>('k')) {
        return "Wrong elements for 'k'..'k': $list5"
    }

    val list6 = ArrayList<UInt>()
    val range6 = 5u..5u
    for (i in range6) {
        list6.add(i)
        if (list6.size > 23) break
    }
    if (list6 != listOf<UInt>(5u)) {
        return "Wrong elements for 5u..5u: $list6"
    }

    val list7 = ArrayList<UInt>()
    val range7 = 5u.toUByte()..5u.toUByte()
    for (i in range7) {
        list7.add(i)
        if (list7.size > 23) break
    }
    if (list7 != listOf<UInt>(5u)) {
        return "Wrong elements for 5u.toUByte()..5u.toUByte(): $list7"
    }

    val list8 = ArrayList<UInt>()
    val range8 = 5u.toUShort()..5u.toUShort()
    for (i in range8) {
        list8.add(i)
        if (list8.size > 23) break
    }
    if (list8 != listOf<UInt>(5u)) {
        return "Wrong elements for 5u.toUShort()..5u.toUShort(): $list8"
    }

    val list9 = ArrayList<ULong>()
    val range9 = 5uL..5uL
    for (i in range9) {
        list9.add(i)
        if (list9.size > 23) break
    }
    if (list9 != listOf<ULong>(5uL)) {
        return "Wrong elements for 5uL..5uL: $list9"
    }

    return "OK"
}
