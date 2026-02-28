import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

// 响应拦截器
api.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 基金相关 API
export const fundApi = {
  // 基金列表
  getFundList(params) {
    return api.get('/funds', { params })
  },
  
  // 基金详情
  getFundDetail(code) {
    return api.get(`/funds/${code}`)
  },
  
  // 搜索建议
  searchSuggest(keyword, limit = 10) {
    return api.get('/funds/search/suggest', { params: { keyword, limit } })
  },
  
  // 基金指标
  getFundMetrics(code) {
    return api.get(`/funds/${code}/metrics`)
  },
  
  // 净值历史
  getFundNav(code, params) {
    return api.get(`/funds/${code}/nav`, { params })
  },
  
  // 交易信号
  getFundSignal(code) {
    return api.get(`/funds/${code}/signal`)
  },
  
  // TOP排名
  getTopFunds(sortBy = 'sharpe', limit = 10) {
    return api.get('/funds/top', { params: { sortBy, limit } })
  },
}

// 持仓相关 API
export const portfolioApi = {
  // 当前持仓
  getHoldings() {
    return api.get('/portfolio/holdings')
  },
  
  // 组合分析
  getAnalysis() {
    return api.get('/portfolio/analysis')
  },
  
  // 记录交易
  recordTrade(data) {
    return api.post('/portfolio/trade', data)
  },
}

export default api
