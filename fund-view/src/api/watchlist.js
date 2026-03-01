import request from '@/utils/request'

/**
 * 添加关注基金
 */
export function addWatchlist(data) {
  return request({
    url: '/watchlist/add',
    method: 'post',
    data
  })
}

/**
 * 获取关注列表
 */
export function getWatchlist(type) {
  return request({
    url: '/watchlist/list',
    method: 'get',
    params: { type }
  })
}

/**
 * 更新关注信息
 */
export function updateWatchlist(fundCode, data) {
  return request({
    url: `/watchlist/${fundCode}`,
    method: 'put',
    data
  })
}

/**
 * 移除关注
 */
export function deleteWatchlist(fundCode) {
  return request({
    url: `/watchlist/${fundCode}`,
    method: 'delete'
  })
}

/**
 * 检查基金是否已关注
 */
export function checkWatchlist(fundCode) {
  return request({
    url: `/watchlist/${fundCode}/check`,
    method: 'get'
  })
}

/**
 * 从持仓导入
 */
export function importFromPortfolio() {
  return request({
    url: '/watchlist/import-from-portfolio',
    method: 'post'
  })
}

/**
 * 获取关注的基金代码列表
 */
export function getWatchedFundCodes() {
  return request({
    url: '/watchlist/codes',
    method: 'get'
  })
}

/**
 * 检查今天是否交易日
 */
export function isTradingDay() {
  return request({
    url: '/trading-calendar/today/is-trading-day',
    method: 'get'
  })
}

/**
 * 检查指定日期是否交易日
 */
export function isTradingDayByDate(date) {
  return request({
    url: '/trading-calendar/is-trading-day',
    method: 'get',
    params: { date }
  })
}

/**
 * 检查当前是否交易时间
 */
export function isTradingTime() {
  return request({
    url: '/trading-calendar/is-trading-time',
    method: 'get'
  })
}

/**
 * 获取上一交易日
 */
export function getPrevTradingDay(date) {
  return request({
    url: '/trading-calendar/prev-trading-day',
    method: 'get',
    params: { date }
  })
}

/**
 * 获取下一交易日
 */
export function getNextTradingDay(date) {
  return request({
    url: '/trading-calendar/next-trading-day',
    method: 'get',
    params: { date }
  })
}

/**
 * 获取当前交易日
 */
export function getCurrentTradeDate() {
  return request({
    url: '/trading-calendar/current-trade-date',
    method: 'get'
  })
}

/**
 * 获取交易状态概览
 */
export function getTradingStatus() {
  return request({
    url: '/trading-calendar/status',
    method: 'get'
  })
}
