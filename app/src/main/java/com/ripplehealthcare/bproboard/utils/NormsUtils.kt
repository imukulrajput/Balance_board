package com.ripplehealthcare.bproboard.utils

import com.ripplehealthcare.bproboard.domain.model.Gender

// Fetch 5-Times STS Norms
fun getFiveSTSNorm(age: Int, gender: Gender): NormValue? {
    return fiveTimesSTSNorms[gender]?.find { age in it.minAge..it.maxAge }
}

// Fetch 30-Sec STS Norms
fun getThirtySTSNorm(age: Int, gender: Gender): NormValue? {
    return thirtySecSTSNorms[gender]?.find { age in it.minAge..it.maxAge }
}

// Fetch TUG Norms
fun getTugNorm(age: Int, gender: Gender): NormValue? {
    return tugNorms[gender]?.find { age in it.minAge..it.maxAge }
}
