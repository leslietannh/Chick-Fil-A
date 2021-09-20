/* eslint-disable */

const locatorType = arguments[0]
const locatorValue = arguments[1]
const command = arguments[2]

let element
switch (locatorType) {
  case 'id':
    element = document.getElementById(locatorValue)
    break
  case 'name':
    element = document.getElementsByName(locatorValue)[0]
    break
  case 'className':
    element = document.getElementsByClassName(locatorValue)[0]
    break
  case 'linkText':
    const xpath = `//a[text()='${locatorValue}']`
    element = document.evaluate(xpath, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue
    break
  case 'css':
    element = document.querySelector(locatorValue)
    break
  case 'xpath':
    element = document.evaluate(locatorValue, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue
    break
}

if (!element) {
  return null
}

switch (command) {
  case "scrollIntoView":
    const elementRect = element.getBoundingClientRect()
    const absoluteElementTop = elementRect.top + window.pageYOffset
    const middle = absoluteElementTop - (window.innerHeight / 2)
    window.scrollTo({
      top: middle,
      left: window.pageXOffset
    })

    return true
  case "getBoundingClientRect":
    const {devicePixelRatio} = window
    const rect = element.getBoundingClientRect()

    return {
      x: Math.round(rect.x * devicePixelRatio),
      y: Math.round(rect.y * devicePixelRatio),
      width: Math.round(rect.width * devicePixelRatio),
      height: Math.round(rect.height * devicePixelRatio),
      windowInnerHeight: Math.round(window.innerHeight * devicePixelRatio)
    }
  default:
    throw new Error(`Unsupported command: ${command}`)
}
