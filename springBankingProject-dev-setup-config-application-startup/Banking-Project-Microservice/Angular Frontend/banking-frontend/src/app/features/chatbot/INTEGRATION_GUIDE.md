# Chatbot Backend Integration Guide

This guide explains how the chatbot is now connected to your real banking microservices.

## 🔗 **Integration Overview**

The chatbot now integrates with your existing microservices through a new `ChatbotApiService` that connects to:

- **Account Service** - Real account data and balances
- **Transaction Service** - Live transaction history
- **Auth Service** - User authentication and context
- **User Profile Service** - KYC status and user information

## 📁 **New Files Created**

### 1. `chatbot-api.service.ts`
- **Purpose**: Handles all API calls to your microservices
- **Features**: 
  - Real account data retrieval
  - Transaction history integration
  - KYC status checking
  - Error handling and fallbacks
  - User context management

### 2. Updated `chatbot.service.ts`
- **Purpose**: Enhanced with real API integration
- **Features**:
  - Real-time data processing
  - API-based message handling
  - Fallback to mock data if APIs fail
  - Context refresh capabilities

### 3. Updated `chatbot.component.ts`
- **Purpose**: Enhanced UI with loading states
- **Features**:
  - Loading indicators
  - Refresh button for data updates
  - Better error handling

## 🔧 **How It Works**

### 1. **Initialization**
```typescript
// When chatbot loads, it automatically:
1. Gets current user ID from AuthService
2. Fetches real account data from AccountService
3. Loads transaction history from TransactionService
4. Gets KYC status from user profile
5. Falls back to mock data if any API fails
```

### 2. **Real-Time Queries**
```typescript
// When user asks "Show my account balance":
1. ChatbotApiService.processAccountBalanceQuery()
2. Calls AccountService.getAccountsByUserId()
3. Formats real account data with proper masking
4. Returns live balance information
```

### 3. **Error Handling**
```typescript
// If API calls fail:
1. Shows user-friendly error messages
2. Falls back to mock data for demonstration
3. Logs errors for debugging
4. Maintains chatbot functionality
```

## 🚀 **API Endpoints Used**

### Account Service Integration
- `GET /accounts/user/{userId}` - Get user's accounts
- `GET /accounts/{accountId}` - Get specific account details
- Real account balances and information

### Transaction Service Integration
- `GET /transactions/user/{userId}` - Get user's transactions
- Real transaction history and statements

### Auth Service Integration
- `getIdentityClaims()` - Get current user information
- `getAccessToken()` - Authentication for API calls
- User context and session management

## 📊 **Data Flow**

```
User Query → ChatbotService → ChatbotApiService → Microservices → Real Data
     ↓
Response Processing → Data Masking → User-Friendly Format → Chatbot UI
```

## 🔒 **Security Features**

### Data Masking
- Account numbers: `****1234` (shows only last 4 digits)
- Sensitive information properly hidden
- Secure data transmission

### Authentication
- Uses existing JWT tokens from AuthService
- Automatic token refresh
- Secure API communication

### Error Handling
- No sensitive data in error messages
- Graceful fallbacks
- User-friendly error responses

## 🎯 **Supported Real Queries**

### Account Information
- ✅ **"Show my account balance"** → Real account balances
- ✅ **"My accounts"** → Real account details
- ✅ **"Account information"** → Live account data

### KYC Status
- ✅ **"Check my KYC status"** → Real verification status
- ✅ **"KYC verification"** → Live KYC information

### Transactions
- ✅ **"Transaction history"** → Real transaction data
- ✅ **"Recent transactions"** → Live transaction list
- ✅ **"Statements"** → Real statement information

### Other Features
- ✅ **Menu navigation** → Works with real data
- ✅ **Breadcrumb tracking** → Real context awareness
- ✅ **Data refresh** → Live data updates

## 🔄 **Real-Time Updates**

### Refresh Button
- Click the refresh icon in chatbot header
- Updates all data from microservices
- Shows latest account balances and transactions

### Automatic Updates
- Context refreshes on new queries
- Always shows latest data
- Maintains conversation history

## 🛠️ **Configuration**

### Environment Setup
```typescript
// environment.ts
export const environment = {
  apiUrl: 'http://localhost:9010', // Your API Gateway
  // ... other config
};
```

### Service Dependencies
```typescript
// The chatbot automatically uses:
- AccountService (existing)
- TransactionService (existing) 
- AuthService (existing)
- No additional configuration needed
```

## 🚨 **Troubleshooting**

### Common Issues

1. **"Unable to retrieve account information"**
   - Check if user is authenticated
   - Verify API Gateway is running
   - Check network connectivity

2. **"No accounts found"**
   - User might not have accounts yet
   - Check Account Service status
   - Verify user ID is correct

3. **"KYC status unknown"**
   - KYC service might be down
   - User profile not loaded
   - Check user authentication

### Debug Mode
```typescript
// Check browser console for:
- API call logs
- Error messages
- Data flow information
- Authentication status
```

## 📈 **Performance Features**

### Loading States
- Spinner during API calls
- Disabled input during processing
- Visual feedback for users

### Caching
- User context cached in memory
- Reduces redundant API calls
- Faster response times

### Error Recovery
- Automatic fallback to mock data
- Graceful error handling
- Maintains chatbot functionality

## 🔮 **Future Enhancements**

### Planned Features
- **Real-time notifications** - WebSocket integration
- **Advanced analytics** - Usage tracking
- **Multi-language support** - Internationalization
- **Voice integration** - Speech-to-text
- **File uploads** - Document sharing

### Backend Integration
- **Loan Service** - Real loan data
- **Card Service** - Credit card information
- **Notification Service** - Live alerts
- **User Service** - Complete profile data

## 📞 **Support**

### Development Support
1. Check console logs for API errors
2. Verify microservice connectivity
3. Test authentication flow
4. Check API Gateway status

### User Support
- Chatbot provides helpful error messages
- Fallback to mock data ensures functionality
- Refresh button for data updates
- Clear breadcrumb navigation

The chatbot is now fully integrated with your banking microservices and provides real-time, secure access to user data while maintaining excellent user experience and error handling.
