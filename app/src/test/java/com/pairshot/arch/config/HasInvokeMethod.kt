package com.pairshot.arch.config

import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaModifier
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent

class HasInvokeMethod : ArchCondition<JavaClass>("have an invoke() method") {
    override fun check(
        clazz: JavaClass,
        events: ConditionEvents,
    ) {
        val hasInvoke =
            clazz.methods.any { method ->
                method.name == "invoke" &&
                    JavaModifier.SYNTHETIC !in method.modifiers
            }
        if (!hasInvoke) {
            events.add(
                SimpleConditionEvent.violated(
                    clazz,
                    "${clazz.name} has no invoke() method",
                ),
            )
        }
    }
}
