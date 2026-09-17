package com.silverguard.app.network

import com.silverguard.app.model.AiAnalysisResult
import com.silverguard.app.model.RiskAnalysis

interface AiRiskAnalysisProvider {
    suspend fun analyze(analysis: RiskAnalysis): AiAnalysisResult
}
