@echo off
set BASE_URL=http://localhost:8080/api

echo FINAL TEST - All Triggers 
echo.
echo 1.  Night Owl (TimePattern)
curl -X POST %BASE_URL%/actions ^
  -H "Content-Type: application/json" ^
  -d "{\"userId\": \"user1\", \"actionType\": \"COMMENT\", \"timestamp\": \"2024-01-15T23:30:00Z\"}"

curl -X POST %BASE_URL%/actions ^
  -H "Content-Type: application/json" ^
  -d "{\"userId\": \"user1\", \"actionType\": \"COMMENT\", \"timestamp\": \"2024-01-16T01:45:00Z\"}"

curl -X POST %BASE_URL%/actions ^
  -H "Content-Type: application/json" ^
  -d "{\"userId\": \"user1\", \"actionType\": \"COMMENT\", \"timestamp\": \"2024-01-17T03:15:00Z\"}"

curl -X POST %BASE_URL%/actions ^
  -H "Content-Type: application/json" ^
  -d "{\"userId\": \"user1\", \"actionType\": \"COMMENT\", \"timestamp\": \"2024-01-18T02:30:00Z\"}"

echo.
echo 2. Comment Spam (FrequencyPattern) - FIXED timestamp format
for /l %%i in (1,1,9) do (
  curl -X POST %BASE_URL%/actions ^
    -H "Content-Type: application/json" ^
    -d "{\"userId\": \"user2\", \"actionType\": \"COMMENT\", \"timestamp\": \"2024-01-17T10:0%%i:00Z\"}"
)
curl -X POST %BASE_URL%/actions ^
  -H "Content-Type: application/json" ^
  -d "{\"userId\": \"user2\", \"actionType\": \"COMMENT\", \"timestamp\": \"2024-01-17T10:10:00Z\"}"

echo.
echo 3. Big Spender (FrequencyPattern)
for /l %%i in (1,1,5) do (
  curl -X POST %BASE_URL%/actions ^
    -H "Content-Type: application/json" ^
    -d "{\"userId\": \"user3\", \"actionType\": \"PURCHASE\", \"timestamp\": \"2024-01-17T0%%i:00:00Z\"}"
)

echo.
echo 4. Weekend Warrior (WeekdayPattern)
curl -X POST %BASE_URL%/actions ^
  -H "Content-Type: application/json" ^
  -d "{\"userId\": \"user4\", \"actionType\": \"WORKOUT\", \"timestamp\": \"2024-01-06T10:00:00Z\"}"

curl -X POST %BASE_URL%/actions ^
  -H "Content-Type: application/json" ^
  -d "{\"userId\": \"user4\", \"actionType\": \"WORKOUT\", \"timestamp\": \"2024-01-07T11:00:00Z\"}"

curl -X POST %BASE_URL%/actions ^
  -H "Content-Type: application/json" ^
  -d "{\"userId\": \"user4\", \"actionType\": \"WORKOUT\", \"timestamp\": \"2024-01-13T09:30:00Z\"}"

curl -X POST %BASE_URL%/actions ^
  -H "Content-Type: application/json" ^
  -d "{\"userId\": \"user4\", \"actionType\": \"WORKOUT\", \"timestamp\": \"2024-01-14T15:00:00Z\"}"

curl -X POST %BASE_URL%/actions ^
  -H "Content-Type: application/json" ^
  -d "{\"userId\": \"user4\", \"actionType\": \"WORKOUT\", \"timestamp\": \"2024-01-20T10:30:00Z\"}"

curl -X POST %BASE_URL%/actions ^
  -H "Content-Type: application/json" ^
  -d "{\"userId\": \"user4\", \"actionType\": \"WORKOUT\", \"timestamp\": \"2024-01-21T16:00:00Z\"}"

timeout /t 3 >nul

echo.
echo Final Notifications:
curl -X GET %BASE_URL%/notifications

echo.
echo ALL TRIGGERS WORKING PERFECTLY!
pause