import request from '@/utils/request'

export function queryWechatJsapiConfigApi(params) {
  return request.get('/wechat/jsapi-config', { params })
}
