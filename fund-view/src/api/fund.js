
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
