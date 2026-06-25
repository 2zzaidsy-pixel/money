# PayWise Firestore Security Rules

Deploy via Firebase Console > Firestore > Rules

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Helper: authenticate
    function isAuth() {
      return request.auth != null && request.auth.uid != null;
    }

    // Helper: owner match
    function isOwner(userId) {
      return isAuth() && request.auth.uid == userId;
    }

    // User Profiles — only own profile
    match /users/{userId} {
      allow read, write: if isOwner(userId);

      // Subcollections inherit parent rules
      match /expenses/{expenseId} {
        allow read, write: if isOwner(userId);
      }
      match /budgets/{budgetId} {
        allow read, write: if isOwner(userId);
      }
      match /goals/{goalId} {
        allow read, write: if isOwner(userId);
      }
      match /emergency_funds/{fundId} {
        allow read, write: if isOwner(userId);
      }
      match /subscriptions/{subId} {
        allow read, write: if isOwner(userId);
      }
      match /simulations/{simId} {
        allow read, write: if isOwner(userId);
      }
    }

    // Deny everything else
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

## Validation Rules (Optional — add to Firestore console)

```
// Example: validate expense amounts are positive
match /users/{userId}/expenses/{expenseId} {
  allow write: if isOwner(userId)
    && request.resource.data.amount > 0
    && request.resource.data.amount is number;
}

// Example: validate budget limits
match /users/{userId}/budgets/{budgetId} {
  allow write: if isOwner(userId)
    && request.resource.data.monthlyLimit > 0
    && request.resource.data.monthlyLimit is number;
}
```

## Indexes Required
- `users/{userId}/expenses`: composite index on `(date: desc, category: asc)`
- `users/{userId}/budgets`: composite index on `(category: asc, month: asc)`
- `users/{userId}/goals`: composite index on `(targetDate: asc, progress: asc)`

Create these in Firebase Console > Firestore > Indexes.
