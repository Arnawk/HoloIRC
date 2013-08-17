package com.fusionx.irc.parser;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.fusionx.irc.Server;
import com.fusionx.irc.constants.ServerCommands;
import com.fusionx.irc.enums.ServerEventType;
import com.fusionx.irc.listeners.CoreListener;
import com.fusionx.irc.misc.Utils;
import com.fusionx.uiircinterface.MessageSender;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import lombok.AccessLevel;
import lombok.Getter;

import static com.fusionx.irc.constants.Constants.LOG_TAG;
import static com.fusionx.irc.misc.Utils.parcelDataForBroadcast;

@Getter(AccessLevel.PACKAGE)
public class ServerLineParser {
    private final Server server;

    @Getter(AccessLevel.NONE)
    private final ServerCodeParser codeParser;
    @Getter(AccessLevel.NONE)
    private final ServerCommandParser commandParser;

    public ServerLineParser(final Context context, final Server server) {
        this.server = server;
        commandParser = new ServerCommandParser(context, this);
        codeParser = new ServerCodeParser(context, this);
    }

    /**
     * A loop which reads each line from the server as it is received and passes it on
     * to parse
     *
     * @param reader - the reader associated with the server stream
     */
    public void parseMain(final BufferedReader reader) {
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                final ServerEventType type = parseLine(line);
                if (type != null && type.equals(ServerEventType.Error)) {
                    return;
                }
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    /**
     * Parses a line from the server
     *
     * @param line - the raw line from the server
     * @return returns an event if there is an error - otherwise returns null
     */
    ServerEventType parseLine(final String line) {
        final ArrayList<String> parsedArray = Utils.splitRawLine(line);
        switch (parsedArray.get(0)) {
            case ServerCommands.Ping: {
                // Immediately return
                final String source = parsedArray.get(1);
                CoreListener.respondToPing(server.getWriter(), source);
                return null;
            }
            case ServerCommands.Error: {
                // We are finished - the server has kicked us out for some reason
                final Bundle event = parcelDataForBroadcast(null,
                        ServerEventType.Error, parsedArray.get(1));
                MessageSender.getSender(server.getTitle()).sendServerMessage(event);
                return ServerEventType.Error;
            }
            default: {
                // Check if the second thing is a code or a command
                if (StringUtils.isNumeric(parsedArray.get(1))) {
                    codeParser.parseCode(parsedArray);
                } else {
                    commandParser.parseCommand(parsedArray, line);
                }
                return null;
            }
        }
    }
}