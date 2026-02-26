FROM node:18-alpine

WORKDIR /app

# Copy package file
COPY package.json ./

# Install dependencies
RUN npm install --production

# Copy server code
COPY server.js .

# Expose port 80
EXPOSE 80

# Start the server
CMD ["npm", "start"]
