require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name         = "RNReactNativeDailyStepCounter"
  s.homepage     = "https://github.com/eatnug"
  s.license      = "MIT"
  # s.license      = { :type => "MIT", :file => "FILE_LICENSE" }
  s.author             = { "author" => "wlq4568@gmail.com" }

  s.source       = { :git => "https://github.com/author/RNReactNativeDailyStepCounter.git", :tag => "master" }
  s.source_files  = "ios/**/*.{h,m}"
  s.requires_arc = true

  s.platforms    = { :ios => "10.0" }

  s.version      = package["version"]
  s.summary      = package["description"]


  s.dependency "React-Core"
end

  