const INVALID_ACCURACY_METERS = 1000
const WEAK_ACCURACY_FLOOR_METERS = 100

function roundCoordinate(value) {
  if (value == null || Number.isNaN(Number(value))) {
    return null
  }
  return Number(Number(value).toFixed(6))
}

function calculateDistanceMeters(latitude1, longitude1, latitude2, longitude2) {
  const values = [latitude1, longitude1, latitude2, longitude2].map(item => Number(item))
  if (values.some(item => Number.isNaN(item))) {
    return null
  }
  const [lat1, lng1, lat2, lng2] = values
  const earthRadiusMeters = 6371000
  const latDistance = ((lat2 - lat1) * Math.PI) / 180
  const lngDistance = ((lng2 - lng1) * Math.PI) / 180
  const sinLat = Math.sin(latDistance / 2)
  const sinLng = Math.sin(lngDistance / 2)
  const a = sinLat * sinLat + Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * sinLng * sinLng
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  return Math.round(earthRadiusMeters * c)
}

function outOfChina(longitude, latitude) {
  return longitude < 72.004 || longitude > 137.8347 || latitude < 0.8293 || latitude > 55.8271
}

function transformLatitude(longitude, latitude) {
  let result = -100 + 2 * longitude + 3 * latitude + 0.2 * latitude * latitude + 0.1 * longitude * latitude + 0.2 * Math.sqrt(Math.abs(longitude))
  result += ((20 * Math.sin(6 * longitude * Math.PI) + 20 * Math.sin(2 * longitude * Math.PI)) * 2) / 3
  result += ((20 * Math.sin(latitude * Math.PI) + 40 * Math.sin((latitude / 3) * Math.PI)) * 2) / 3
  result += ((160 * Math.sin((latitude / 12) * Math.PI) + 320 * Math.sin((latitude * Math.PI) / 30)) * 2) / 3
  return result
}

function transformLongitude(longitude, latitude) {
  let result = 300 + longitude + 2 * latitude + 0.1 * longitude * longitude + 0.1 * longitude * latitude + 0.1 * Math.sqrt(Math.abs(longitude))
  result += ((20 * Math.sin(6 * longitude * Math.PI) + 20 * Math.sin(2 * longitude * Math.PI)) * 2) / 3
  result += ((20 * Math.sin(longitude * Math.PI) + 40 * Math.sin((longitude / 3) * Math.PI)) * 2) / 3
  result += ((150 * Math.sin((longitude / 12) * Math.PI) + 300 * Math.sin((longitude / 30) * Math.PI)) * 2) / 3
  return result
}

function wgs84ToGcj02(longitude, latitude) {
  const lng = Number(longitude)
  const lat = Number(latitude)
  if (Number.isNaN(lng) || Number.isNaN(lat) || outOfChina(lng, lat)) {
    return { longitude: lng, latitude: lat }
  }
  const a = 6378245.0
  const ee = 0.00669342162296594323
  let dLat = transformLatitude(lng - 105.0, lat - 35.0)
  let dLng = transformLongitude(lng - 105.0, lat - 35.0)
  const radLat = (lat / 180.0) * Math.PI
  let magic = Math.sin(radLat)
  magic = 1 - ee * magic * magic
  const sqrtMagic = Math.sqrt(magic)
  dLat = (dLat * 180.0) / (((a * (1 - ee)) / (magic * sqrtMagic)) * Math.PI)
  dLng = (dLng * 180.0) / ((a / sqrtMagic) * Math.cos(radLat) * Math.PI)
  return {
    longitude: lng + dLng,
    latitude: lat + dLat
  }
}

function isInvalidGeolocationSample(position) {
  const latitude = Number(position?.coords?.latitude)
  const longitude = Number(position?.coords?.longitude)
  const accuracyMeters = position?.coords?.accuracy == null ? null : Math.round(Number(position.coords.accuracy))
  if (Number.isNaN(latitude) || Number.isNaN(longitude)) {
    return true
  }
  if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
    return true
  }
  if (Math.abs(latitude) < 0.000001 && Math.abs(longitude) < 0.000001) {
    return true
  }
  return accuracyMeters != null && accuracyMeters > INVALID_ACCURACY_METERS
}

function isWeakAccuracy(accuracyMeters, radiusMeters) {
  if (accuracyMeters == null) {
    return false
  }
  return accuracyMeters > WEAK_ACCURACY_FLOOR_METERS && accuracyMeters <= INVALID_ACCURACY_METERS
}

export function buildCheckInDiagnosticsFromCoordinates({
  latitude,
  longitude,
  accuracyMeters = null,
  timestamp = Date.now(),
  coordinateType = 'wgs84',
  targetLatitude = null,
  targetLongitude = null,
  radiusMeters = null
} = {}) {
  const rawLatitude = Number(latitude)
  const rawLongitude = Number(longitude)
  const normalizedAccuracy = accuracyMeters == null ? null : Math.round(Number(accuracyMeters))
  const normalizedTimestamp = Number(timestamp || Date.now())
  const normalizedTargetLatitude = targetLatitude == null ? undefined : Number(targetLatitude)
  const normalizedTargetLongitude = targetLongitude == null ? undefined : Number(targetLongitude)
  const normalizedRadiusMeters = radiusMeters == null ? null : Number(radiusMeters)
  const rawDistanceMeters = calculateDistanceMeters(rawLatitude, rawLongitude, normalizedTargetLatitude, normalizedTargetLongitude)
  const isGcj02 = String(coordinateType || '').toLowerCase() === 'gcj02'
  const converted = isGcj02
    ? { longitude: rawLongitude, latitude: rawLatitude }
    : wgs84ToGcj02(rawLongitude, rawLatitude)
  const convertedDistanceMeters = calculateDistanceMeters(converted.latitude, converted.longitude, normalizedTargetLatitude, normalizedTargetLongitude)
  const useConverted = !isGcj02
    && rawDistanceMeters != null
    && convertedDistanceMeters != null
    && convertedDistanceMeters < rawDistanceMeters
  const submitLatitude = roundCoordinate(isGcj02 ? rawLatitude : (useConverted ? converted.latitude : rawLatitude))
  const submitLongitude = roundCoordinate(isGcj02 ? rawLongitude : (useConverted ? converted.longitude : rawLongitude))
  const localDistanceMeters = calculateDistanceMeters(submitLatitude, submitLongitude, normalizedTargetLatitude, normalizedTargetLongitude)
  const overlap = localDistanceMeters != null
    && normalizedRadiusMeters != null
    && normalizedAccuracy != null
    && localDistanceMeters <= normalizedRadiusMeters + normalizedAccuracy

  return {
    rawLatitude: roundCoordinate(rawLatitude),
    rawLongitude: roundCoordinate(rawLongitude),
    convertedLatitude: roundCoordinate(converted.latitude),
    convertedLongitude: roundCoordinate(converted.longitude),
    submitLatitude,
    submitLongitude,
    targetLatitude: roundCoordinate(normalizedTargetLatitude),
    targetLongitude: roundCoordinate(normalizedTargetLongitude),
    radiusMeters: normalizedRadiusMeters,
    accuracyMeters: normalizedAccuracy,
    timestamp: normalizedTimestamp,
    rawDistanceMeters,
    convertedDistanceMeters,
    localDistanceMeters,
    overlap,
    coordinateSource: isGcj02 ? 'wechat-gcj02' : (useConverted ? 'gcj02-adjusted' : 'browser-raw')
  }
}

function buildCheckInDiagnostics(position, target = {}) {
  return buildCheckInDiagnosticsFromCoordinates({
    latitude: position?.coords?.latitude,
    longitude: position?.coords?.longitude,
    accuracyMeters: position?.coords?.accuracy,
    timestamp: position?.timestamp,
    coordinateType: 'wgs84',
    targetLatitude: target.targetLatitude,
    targetLongitude: target.targetLongitude,
    radiusMeters: target.radiusMeters
  })
}

function compareCheckInDiagnostics(current, next) {
  const currentWeak = isWeakAccuracy(current.accuracyMeters, current.radiusMeters)
  const nextWeak = isWeakAccuracy(next.accuracyMeters, next.radiusMeters)
  if (currentWeak !== nextWeak) {
    return currentWeak ? 1 : -1
  }
  const currentAccuracy = current.accuracyMeters ?? Number.POSITIVE_INFINITY
  const nextAccuracy = next.accuracyMeters ?? Number.POSITIVE_INFINITY
  if (currentAccuracy !== nextAccuracy) {
    return currentAccuracy - nextAccuracy
  }
  const currentTimestamp = current.timestamp ?? 0
  const nextTimestamp = next.timestamp ?? 0
  if (currentTimestamp !== nextTimestamp) {
    return nextTimestamp - currentTimestamp
  }
  const currentDistance = current.localDistanceMeters ?? Number.POSITIVE_INFINITY
  const nextDistance = next.localDistanceMeters ?? Number.POSITIVE_INFINITY
  return currentDistance - nextDistance
}

function isLocalhostHost(hostname) {
  return hostname === 'localhost' || hostname === '127.0.0.1' || hostname === '::1'
}

function isPrivateIpv4Host(hostname) {
  return /^10\./.test(hostname)
    || /^192\.168\./.test(hostname)
    || /^172\.(1[6-9]|2\d|3[0-1])\./.test(hostname)
}

export function buildLocationEnvironmentDiagnostics() {
  if (typeof window === 'undefined' || typeof navigator === 'undefined') {
    return {
      userAgent: '',
      url: '',
      protocol: '',
      hostname: '',
      isHttps: false,
      isLocalhost: false,
      isIntranet: false
    }
  }
  const { protocol, hostname, href } = window.location
  return {
    userAgent: navigator.userAgent,
    url: href,
    protocol,
    hostname,
    isHttps: protocol === 'https:',
    isLocalhost: isLocalhostHost(hostname),
    isIntranet: isLocalhostHost(hostname) || isPrivateIpv4Host(hostname) || (!hostname.includes('.') && hostname !== '')
  }
}

function getCurrentPositionOnce({ geolocation, timeoutMs }) {
  return new Promise((resolve, reject) => {
    geolocation.getCurrentPosition(resolve, reject, {
      enableHighAccuracy: true,
      timeout: timeoutMs,
      maximumAge: 0
    })
  })
}

export async function getUserLocation({
  geolocation = typeof navigator !== 'undefined' ? navigator.geolocation : null,
  targetLatitude = null,
  targetLongitude = null,
  radiusMeters = null,
  sampleCount = 3,
  timeoutMs = 8000
} = {}) {
  if (!geolocation?.getCurrentPosition) {
    return {
      latitude: null,
      longitude: null,
      accuracy: null,
      errorCode: 'UNSUPPORTED',
      errorMessage: '当前环境不支持定位',
      diagnostics: null,
      samples: [],
      invalidReason: '当前环境不支持定位',
      lastError: null
    }
  }

  const samples = []
  let lastError = null
  for (let index = 0; index < sampleCount; index += 1) {
    try {
      const position = await getCurrentPositionOnce({ geolocation, timeoutMs })
      const diagnostics = buildCheckInDiagnostics(position, {
        targetLatitude,
        targetLongitude,
        radiusMeters
      })
      diagnostics.sampleIndex = index + 1
      diagnostics.invalidSample = isInvalidGeolocationSample(position)
      diagnostics.weakSample = isWeakAccuracy(diagnostics.accuracyMeters, diagnostics.radiusMeters)
      samples.push(diagnostics)
    } catch (error) {
      lastError = error
      if (error?.code === 1) {
        throw error
      }
    }
  }

  const validSamples = samples.filter(item => !item.invalidSample)
  if (!validSamples.length) {
    return {
      latitude: null,
      longitude: null,
      accuracy: null,
      errorCode: lastError?.code ?? 'UNAVAILABLE',
      errorMessage: '',
      diagnostics: null,
      samples,
      invalidReason: '',
      lastError
    }
  }

  validSamples.sort(compareCheckInDiagnostics)
  const selected = validSamples[0]
  return {
    latitude: selected.submitLatitude,
    longitude: selected.submitLongitude,
    accuracy: selected.accuracyMeters,
    errorCode: null,
    errorMessage: '',
    diagnostics: selected,
    samples,
    invalidReason: '',
    lastError
  }
}

export function createTestEnvironmentLocationFallback({
  targetLatitude,
  targetLongitude,
  radiusMeters = null,
  reason = '当前未获取到定位，可继续打卡（测试环境）'
} = {}) {
  const latitude = roundCoordinate(targetLatitude)
  const longitude = roundCoordinate(targetLongitude)
  if (latitude == null || longitude == null) {
    return null
  }
  return {
    latitude,
    longitude,
    accuracy: null,
    errorCode: 'TEST_FALLBACK',
    errorMessage: reason,
    source: 'TEST_FALLBACK',
    diagnostics: {
      rawLatitude: null,
      rawLongitude: null,
      convertedLatitude: null,
      convertedLongitude: null,
      submitLatitude: latitude,
      submitLongitude: longitude,
      targetLatitude: latitude,
      targetLongitude: longitude,
      radiusMeters: radiusMeters == null ? null : Number(radiusMeters),
      accuracyMeters: null,
      timestamp: Date.now(),
      rawDistanceMeters: null,
      convertedDistanceMeters: null,
      localDistanceMeters: 0,
      overlap: true,
      coordinateSource: 'test-environment-fallback'
    }
  }
}
