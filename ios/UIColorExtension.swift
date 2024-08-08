//
//  UIColorExtension.swift
//  VisionSdk
//
//  Created by Ameer Hamza on 24/04/2024.
//  Copyright © 2024 Facebook. All rights reserved.
//

import Foundation

extension UIColor {
    convenience init?(hex: String, alpha: CGFloat = 1.0) {
        var formattedHex = hex.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()

        if formattedHex.hasPrefix("#") {
            formattedHex.remove(at: formattedHex.startIndex)
        }

        if formattedHex.count != 6 {
            return nil
        }

        var rgbValue: UInt64 = 0
        Scanner(string: formattedHex).scanHexInt64(&rgbValue)

        let red = CGFloat((rgbValue & 0xFF0000) >> 16) / 255.0
        let green = CGFloat((rgbValue & 0x00FF00) >> 8) / 255.0
        let blue = CGFloat(rgbValue & 0x0000FF) / 255.0

        self.init(red: red, green: green, blue: blue, alpha: alpha)
    }
}
