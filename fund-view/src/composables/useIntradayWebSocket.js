import { ref, onMounted, onUnmounted, watch } from 'vue'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

/**
 * 实时估值WebSocket composable
 * @param {string} fundCode - 基金代码（可选，用于详情页）
 * @returns {Object} - WebSocket状态和操作方法
 */
export function useIntradayWebSocket(fundCode = null) {
  const client = ref(null)
  const connected = ref(false)
  const estimate = ref(null)
  const error = ref(null)
  
  // WebSocket服务器地址
  const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws'
  
  // 连接WebSocket
  const connect = () => {
    if (client.value?.active) {
      console.log('WebSocket已连接')
      return
    }
    
    const stompClient = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      debug: (str) => {
        if (import.meta.env.DEV) {
          console.log('STOMP:', str)
        }
      },
      reconnectDelay: 5000,  // 5秒后重连
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    })
    
    stompClient.onConnect = (frame) => {
      console.log('WebSocket连接成功:', frame)
      connected.value = true
      error.value = null
      
      // 如果指定了基金代码，订阅该基金的实时估值
      if (fundCode) {
        subscribeFund(fundCode)
      }
    }
    
    stompClient.onDisconnect = () => {
      console.log('WebSocket断开连接')
      connected.value = false
    }
    
    stompClient.onStompError = (frame) => {
      console.error('STOMP错误:', frame.headers.message)
      error.value = frame.headers.message
    }
    
    stompClient.activate()
    client.value = stompClient
  }
  
  // 断开WebSocket
  const disconnect = () => {
    if (client.value?.active) {
      client.value.deactivate()
    }
  }
  
  // 订阅基金详情页的实时估值
  const subscribeFund = (code) => {
    if (!client.value?.active) {
      console.warn('WebSocket未连接，无法订阅')
      return
    }
    
    // 订阅主题 /topic/fund/{fundCode}/intraday
    const subscription = client.value.subscribe(
      `/topic/fund/${code}/intraday`,
      (message) => {
        const data = JSON.parse(message.body)
        estimate.value = data
        console.log('收到估值更新:', data)
      }
    )
    
    // 发送订阅消息
    client.value.publish({
      destination: `/app/fund/${code}/subscribe`,
      body: JSON.stringify({ fundCode: code })
    })
    
    return subscription
  }
  
  // 取消订阅基金
  const unsubscribeFund = (code) => {
    if (!client.value?.active) return
    
    client.value.publish({
      destination: `/app/fund/${code}/unsubscribe`,
      body: JSON.stringify({ fundCode: code })
    })
  }
  
  // 订阅持仓页面的实时估值
  const subscribePortfolio = () => {
    if (!client.value?.active) {
      console.warn('WebSocket未连接，无法订阅')
      return
    }
    
    // 订阅用户队列 /user/queue/portfolio/intraday
    const subscription = client.value.subscribe(
      '/user/queue/portfolio/intraday',
      (message) => {
        const data = JSON.parse(message.body)
        estimate.value = data
        console.log('收到持仓估值更新:', data)
      }
    )
    
    // 发送订阅消息
    client.value.publish({
      destination: '/app/portfolio/subscribe',
      body: '{}'
    })
    
    return subscription
  }
  
  onMounted(() => {
    connect()
  })
  
  onUnmounted(() => {
    disconnect()
  })
  
  return {
    connected,
    estimate,
    error,
    connect,
    disconnect,
    subscribeFund,
    unsubscribeFund,
    subscribePortfolio
  }
}
