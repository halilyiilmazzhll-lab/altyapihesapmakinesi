package com.example.egimhesabi.domain

data class FoseptikResult(
    val inletElev: Double? = null,
    val coverElev: Double? = null,
    val bottomElev: Double? = null,
    val excavationDepth: Double? = null,
    val slopeDecimal: Double? = null,
    val errorMessage: String? = null
)

object FoseptikCalculator {
    fun calculate(
        manholeInvert: Double?,
        distance: Double?,
        slopeType: Int,
        slopeVal: Double?,
        groundElev: Double?,
        tankHeight: Double?,
        coverToInlet: Double?
    ): FoseptikResult {
        if (manholeInvert == null || distance == null || slopeVal == null) {
            return FoseptikResult()
        }
        
        if (distance < 0) {
            return FoseptikResult(errorMessage = "Mesafe negatif olamaz.")
        }

        val slopeDecimal = when (slopeType) {
            0 -> if (slopeVal == 0.0) 0.0 else 1.0 / slopeVal
            1 -> slopeVal / 100.0
            else -> slopeVal / 100.0 // cm/m is same as %
        }

        val inletElev = manholeInvert - (distance * slopeDecimal)

        if (tankHeight != null && coverToInlet != null) {
            if (tankHeight < 0 || coverToInlet < 0) {
                return FoseptikResult(
                    inletElev = inletElev,
                    slopeDecimal = slopeDecimal,
                    errorMessage = "Foseptik boyutları negatif olamaz."
                )
            }
            if (coverToInlet > tankHeight) {
                return FoseptikResult(
                    inletElev = inletElev,
                    slopeDecimal = slopeDecimal,
                    errorMessage = "Kapak-akar mesafesi, toplam foseptik boyundan büyük olamaz."
                )
            }
            
            val coverElev = inletElev + coverToInlet
            val bottomElev = coverElev - tankHeight
            val exc = if (groundElev != null) groundElev - bottomElev else null
            
            return FoseptikResult(
                inletElev = inletElev,
                coverElev = coverElev,
                bottomElev = bottomElev,
                excavationDepth = exc,
                slopeDecimal = slopeDecimal
            )
        }
        
        return FoseptikResult(inletElev = inletElev, slopeDecimal = slopeDecimal)
    }
}
