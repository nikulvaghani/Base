package com.base.utils

/**
 * Created by Nikul on 31-03-2020.
 * Exclude the marked element from Gson's processing logic.
 * <p>
 * This annotation can be used in multiple places where Gson Serialize or Parcelize processor runs. For instance, you can
 * add it to a field. and Gson will not include that field.
 */
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Exclude