import request from '@/utils/request'

/**
 * 获取基金当日分时数据
 */
export function getIntradayData(fundCode) {
  return request({
    url: `/fund/${fundCode}/intraday`,
    method: 'get'
  })
}

/**
 * 手动刷新估值
 */
export function refreshEstimate(fundCode) {
  return request({
    url: `/fund/${fundCode}/estimate/refresh`,
    method: 'post'
  })
}

/**
 * 获取最新估值
 */
export function getLatestEstimate(fundCode) {
  return request({
    url: `/fund/${fundCode}/estimate/latest`,
    method: 'get'
  })
}

/**
 * 搜索基金
 */
export function searchFund(keyword) {
  return request({
    url: '/fund/search',
    method: 'get',
    params: { keyword }
  })
}
