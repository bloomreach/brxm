/*
 * Copyright 2011 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// $ANTLR 3.2 Sep 23, 2009 12:02:23 /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g 2010-06-04 12:36:06


package org.onehippo.cms7.essentials.dashboard.utils.cnd;


import org.antlr.runtime.BaseRecognizer;
import org.antlr.runtime.BitSet;
import org.antlr.runtime.DFA;
import org.antlr.runtime.EarlyExitException;
import org.antlr.runtime.IntStream;
import org.antlr.runtime.MismatchedSetException;
import org.antlr.runtime.NoViableAltException;
import org.antlr.runtime.Parser;
import org.antlr.runtime.ParserRuleReturnScope;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.RecognizerSharedState;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenStream;
import org.antlr.runtime.tree.CommonTreeAdaptor;
import org.antlr.runtime.tree.RewriteRuleSubtreeStream;
import org.antlr.runtime.tree.RewriteRuleTokenStream;
import org.antlr.runtime.tree.TreeAdaptor;

public class CndParser extends Parser {
    public static final String[] tokenNames = new String[]{
            "<invalid>", "<EOR>", "<DOWN>", "<UP>", "NAMESPACES", "PREFIX", "URI", "NODE", "NAME", "PRIMARY_TYPE", "SUPERTYPES", "NODE_TYPES", "NODE_TYPE_ATTRIBUTES", "HAS_ORDERABLE_CHILD_NODES", "IS_MIXIN", "IS_ABSTRACT", "IS_QUERYABLE", "PRIMARY_ITEM_NAME", "PROPERTY_DEFINITION", "REQUIRED_TYPE", "DEFAULT_VALUES", "VALUE_CONSTRAINTS", "AUTO_CREATED", "MANDATORY", "PROTECTED", "ON_PARENT_VERSION", "MULTIPLE", "IS_PRIMARY_PROPERTY", "QUERY_OPERATORS", "IS_FULL_TEXT_SEARCHABLE", "IS_QUERY_ORDERERABLE", "CHILD_NODE_DEFINITION", "REQUIRED_PRIMARY_TYPES", "DEFAULT_PRIMARY_TYPE", "SAME_NAME_SIBLINGS", "STRING", "MULTI_LINE_COMMENT", "SINGLE_LINE_COMMENT", "QUOTED_STRING", "UNQUOTED_STRING", "EscapeSequence", "WS", "'<'", "'='", "'>'", "'mix'", "'['", "']'", "'o'", "'ord'", "'orderable'", "'m'", "'mixin'", "'a'", "'abs'", "'abstract'", "'nq'", "'noquery'", "'primaryitem'", "'!'", "'-'", "'*'", "'('", "')'", "'string'", "'binary'", "'long'", "'double'", "'boolean'", "'decimal'", "'date'", "'name'", "'path'", "'reference'", "'undefined'", "'weakreference'", "'uri'", "'pri'", "'primary'", "'aut'", "'autocreated'", "'man'", "'mandatory'", "'p'", "'pro'", "'protected'", "'copy'", "'version'", "'initialize'", "'compute'", "'ignore'", "'abort'", "'mul'", "'multiple'", "'nof'", "'nofulltext'", "'nqord'", "'noqueryorder'", "'qop'", "'queryops'", "'+'", "'sns'", "','"
    };
    public static final int PREFIX = 5;
    public static final int IS_QUERY_ORDERERABLE = 30;
    public static final int EOF = -1;
    public static final int T__93 = 93;
    public static final int T__94 = 94;
    public static final int T__91 = 91;
    public static final int T__92 = 92;
    public static final int NAME = 8;
    public static final int T__90 = 90;
    public static final int IS_FULL_TEXT_SEARCHABLE = 29;
    public static final int DEFAULT_VALUES = 20;
    public static final int REQUIRED_PRIMARY_TYPES = 32;
    public static final int T__99 = 99;
    public static final int T__98 = 98;
    public static final int T__97 = 97;
    public static final int T__96 = 96;
    public static final int PRIMARY_ITEM_NAME = 17;
    public static final int T__95 = 95;
    public static final int T__80 = 80;
    public static final int T__81 = 81;
    public static final int T__82 = 82;
    public static final int NODE = 7;
    public static final int T__83 = 83;
    public static final int PRIMARY_TYPE = 9;
    public static final int NODE_TYPE_ATTRIBUTES = 12;
    public static final int NODE_TYPES = 11;
    public static final int T__85 = 85;
    public static final int T__84 = 84;
    public static final int T__87 = 87;
    public static final int T__86 = 86;
    public static final int T__89 = 89;
    public static final int AUTO_CREATED = 22;
    public static final int T__88 = 88;
    public static final int URI = 6;
    public static final int WS = 41;
    public static final int T__71 = 71;
    public static final int T__72 = 72;
    public static final int T__70 = 70;
    public static final int ON_PARENT_VERSION = 25;
    public static final int IS_QUERYABLE = 16;
    public static final int PROPERTY_DEFINITION = 18;
    public static final int T__76 = 76;
    public static final int T__75 = 75;
    public static final int IS_MIXIN = 14;
    public static final int T__74 = 74;
    public static final int EscapeSequence = 40;
    public static final int T__73 = 73;
    public static final int T__79 = 79;
    public static final int T__78 = 78;
    public static final int T__77 = 77;
    public static final int HAS_ORDERABLE_CHILD_NODES = 13;
    public static final int T__68 = 68;
    public static final int T__69 = 69;
    public static final int T__66 = 66;
    public static final int PROTECTED = 24;
    public static final int T__67 = 67;
    public static final int SINGLE_LINE_COMMENT = 37;
    public static final int T__64 = 64;
    public static final int T__65 = 65;
    public static final int T__62 = 62;
    public static final int T__63 = 63;
    public static final int DEFAULT_PRIMARY_TYPE = 33;
    public static final int VALUE_CONSTRAINTS = 21;
    public static final int MANDATORY = 23;
    public static final int T__61 = 61;
    public static final int T__60 = 60;
    public static final int QUERY_OPERATORS = 28;
    public static final int MULTIPLE = 26;
    public static final int T__55 = 55;
    public static final int T__56 = 56;
    public static final int IS_PRIMARY_PROPERTY = 27;
    public static final int T__57 = 57;
    public static final int T__58 = 58;
    public static final int T__51 = 51;
    public static final int T__52 = 52;
    public static final int T__53 = 53;
    public static final int T__54 = 54;
    public static final int T__59 = 59;
    public static final int QUOTED_STRING = 38;
    public static final int T__50 = 50;
    public static final int T__42 = 42;
    public static final int T__43 = 43;
    public static final int T__46 = 46;
    public static final int T__47 = 47;
    public static final int T__44 = 44;
    public static final int T__45 = 45;
    public static final int T__48 = 48;
    public static final int CHILD_NODE_DEFINITION = 31;
    public static final int T__49 = 49;
    public static final int T__102 = 102;
    public static final int T__101 = 101;
    public static final int T__100 = 100;
    public static final int SAME_NAME_SIBLINGS = 34;
    public static final int MULTI_LINE_COMMENT = 36;
    public static final int NAMESPACES = 4;
    public static final int SUPERTYPES = 10;
    public static final int IS_ABSTRACT = 15;
    public static final int UNQUOTED_STRING = 39;
    public static final int STRING = 35;
    public static final int REQUIRED_TYPE = 19;

    // delegates
    // delegators


    public CndParser(TokenStream input) {
        this(input, new RecognizerSharedState());
    }

    public CndParser(TokenStream input, RecognizerSharedState state) {
        super(input, state);

    }

    protected TreeAdaptor adaptor = new CommonTreeAdaptor();

    public void setTreeAdaptor(TreeAdaptor adaptor) {
        this.adaptor = adaptor;
    }

    public TreeAdaptor getTreeAdaptor() {
        return adaptor;
    }

    public String[] getTokenNames() {
        return CndParser.tokenNames;
    }

    public String getGrammarFileName() {
        return "/home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g";
    }


    @Override
    public void emitErrorMessage(String msg) {
        System.out.print(msg);
    }


    public static class cnd_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "cnd"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:67:1: cnd : ( namespaceMapping | nodeTypeDefinition )* EOF -> ( ^( NAMESPACES ( namespaceMapping )* ) )? ( ^( NODE_TYPES ( nodeTypeDefinition )* ) )? ;
    public final CndParser.cnd_return cnd() throws RecognitionException {
        CndParser.cnd_return retval = new CndParser.cnd_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token EOF3 = null;
        CndParser.namespaceMapping_return namespaceMapping1 = null;

        CndParser.nodeTypeDefinition_return nodeTypeDefinition2 = null;


        Object EOF3_tree = null;
        RewriteRuleTokenStream stream_EOF = new RewriteRuleTokenStream(adaptor, "token EOF");
        RewriteRuleSubtreeStream stream_nodeTypeDefinition = new RewriteRuleSubtreeStream(adaptor, "rule nodeTypeDefinition");
        RewriteRuleSubtreeStream stream_namespaceMapping = new RewriteRuleSubtreeStream(adaptor, "rule namespaceMapping");
        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:67:5: ( ( namespaceMapping | nodeTypeDefinition )* EOF -> ( ^( NAMESPACES ( namespaceMapping )* ) )? ( ^( NODE_TYPES ( nodeTypeDefinition )* ) )? )
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:67:7: ( namespaceMapping | nodeTypeDefinition )* EOF
            {
                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:67:7: ( namespaceMapping | nodeTypeDefinition )*
                loop1:
                do {
                    int alt1 = 3;
                    int LA1_0 = input.LA(1);

                    if ((LA1_0 == 42)) {
                        alt1 = 1;
                    } else if ((LA1_0 == 46)) {
                        alt1 = 2;
                    }


                    switch (alt1) {
                        case 1:
                            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:67:8: namespaceMapping
                        {
                            pushFollow(FOLLOW_namespaceMapping_in_cnd283);
                            namespaceMapping1 = namespaceMapping();

                            state._fsp--;
                            if (state.failed) return retval;
                            if (state.backtracking == 0) stream_namespaceMapping.add(namespaceMapping1.getTree());

                        }
                        break;
                        case 2:
                            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:67:25: nodeTypeDefinition
                        {
                            pushFollow(FOLLOW_nodeTypeDefinition_in_cnd285);
                            nodeTypeDefinition2 = nodeTypeDefinition();

                            state._fsp--;
                            if (state.failed) return retval;
                            if (state.backtracking == 0) stream_nodeTypeDefinition.add(nodeTypeDefinition2.getTree());

                        }
                        break;

                        default:
                            break loop1;
                    }
                } while (true);

                EOF3 = (Token) match(input, EOF, FOLLOW_EOF_in_cnd289);
                if (state.failed) return retval;
                if (state.backtracking == 0) stream_EOF.add(EOF3);


                // AST REWRITE
                // elements: namespaceMapping, nodeTypeDefinition
                // token labels: 
                // rule labels: retval
                // token list labels: 
                // rule list labels: 
                // wildcard labels: 
                if (state.backtracking == 0) {
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "rule retval", retval != null ? retval.tree : null);

                    root_0 = (Object) adaptor.nil();
                    // 68:5: -> ( ^( NAMESPACES ( namespaceMapping )* ) )? ( ^( NODE_TYPES ( nodeTypeDefinition )* ) )?
                    {
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:68:8: ( ^( NAMESPACES ( namespaceMapping )* ) )?
                        if (stream_namespaceMapping.hasNext()) {
                            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:68:8: ^( NAMESPACES ( namespaceMapping )* )
                            {
                                Object root_1 = (Object) adaptor.nil();
                                root_1 = (Object) adaptor.becomeRoot((Object) adaptor.create(NAMESPACES, "NAMESPACES"), root_1);

                                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:68:21: ( namespaceMapping )*
                                while (stream_namespaceMapping.hasNext()) {
                                    adaptor.addChild(root_1, stream_namespaceMapping.nextTree());

                                }
                                stream_namespaceMapping.reset();

                                adaptor.addChild(root_0, root_1);
                            }

                        }
                        stream_namespaceMapping.reset();
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:68:41: ( ^( NODE_TYPES ( nodeTypeDefinition )* ) )?
                        if (stream_nodeTypeDefinition.hasNext()) {
                            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:68:41: ^( NODE_TYPES ( nodeTypeDefinition )* )
                            {
                                Object root_1 = (Object) adaptor.nil();
                                root_1 = (Object) adaptor.becomeRoot((Object) adaptor.create(NODE_TYPES, "NODE_TYPES"), root_1);

                                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:68:54: ( nodeTypeDefinition )*
                                while (stream_nodeTypeDefinition.hasNext()) {
                                    adaptor.addChild(root_1, stream_nodeTypeDefinition.nextTree());

                                }
                                stream_nodeTypeDefinition.reset();

                                adaptor.addChild(root_0, root_1);
                            }

                        }
                        stream_nodeTypeDefinition.reset();

                    }

                    retval.tree = root_0;
                }
            }

            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "cnd"

    public static class namespaceMapping_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "namespaceMapping"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:71:1: namespaceMapping : '<' prefix '=' uri '>' -> ^( NODE prefix uri ) ;
    public final CndParser.namespaceMapping_return namespaceMapping() throws RecognitionException {
        CndParser.namespaceMapping_return retval = new CndParser.namespaceMapping_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token char_literal4 = null;
        Token char_literal6 = null;
        Token char_literal8 = null;
        CndParser.prefix_return prefix5 = null;

        CndParser.uri_return uri7 = null;


        Object char_literal4_tree = null;
        Object char_literal6_tree = null;
        Object char_literal8_tree = null;
        RewriteRuleTokenStream stream_43 = new RewriteRuleTokenStream(adaptor, "token 43");
        RewriteRuleTokenStream stream_44 = new RewriteRuleTokenStream(adaptor, "token 44");
        RewriteRuleTokenStream stream_42 = new RewriteRuleTokenStream(adaptor, "token 42");
        RewriteRuleSubtreeStream stream_prefix = new RewriteRuleSubtreeStream(adaptor, "rule prefix");
        RewriteRuleSubtreeStream stream_uri = new RewriteRuleSubtreeStream(adaptor, "rule uri");
        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:71:18: ( '<' prefix '=' uri '>' -> ^( NODE prefix uri ) )
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:71:20: '<' prefix '=' uri '>'
            {
                char_literal4 = (Token) match(input, 42, FOLLOW_42_in_namespaceMapping321);
                if (state.failed) return retval;
                if (state.backtracking == 0) stream_42.add(char_literal4);

                pushFollow(FOLLOW_prefix_in_namespaceMapping323);
                prefix5 = prefix();

                state._fsp--;
                if (state.failed) return retval;
                if (state.backtracking == 0) stream_prefix.add(prefix5.getTree());
                char_literal6 = (Token) match(input, 43, FOLLOW_43_in_namespaceMapping325);
                if (state.failed) return retval;
                if (state.backtracking == 0) stream_43.add(char_literal6);

                pushFollow(FOLLOW_uri_in_namespaceMapping327);
                uri7 = uri();

                state._fsp--;
                if (state.failed) return retval;
                if (state.backtracking == 0) stream_uri.add(uri7.getTree());
                char_literal8 = (Token) match(input, 44, FOLLOW_44_in_namespaceMapping329);
                if (state.failed) return retval;
                if (state.backtracking == 0) stream_44.add(char_literal8);


                // AST REWRITE
                // elements: uri, prefix
                // token labels: 
                // rule labels: retval
                // token list labels: 
                // rule list labels: 
                // wildcard labels: 
                if (state.backtracking == 0) {
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "rule retval", retval != null ? retval.tree : null);

                    root_0 = (Object) adaptor.nil();
                    // 71:43: -> ^( NODE prefix uri )
                    {
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:71:46: ^( NODE prefix uri )
                        {
                            Object root_1 = (Object) adaptor.nil();
                            root_1 = (Object) adaptor.becomeRoot((Object) adaptor.create(NODE, "NODE"), root_1);

                            adaptor.addChild(root_1, stream_prefix.nextTree());
                            adaptor.addChild(root_1, stream_uri.nextTree());

                            adaptor.addChild(root_0, root_1);
                        }

                    }

                    retval.tree = root_0;
                }
            }

            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "namespaceMapping"

    public static class prefix_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "prefix"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:72:1: prefix : ( | 'mix' -> ^( PREFIX 'mix' ) | STRING -> ^( PREFIX STRING ) );
    public final CndParser.prefix_return prefix() throws RecognitionException {
        CndParser.prefix_return retval = new CndParser.prefix_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token string_literal9 = null;
        Token STRING10 = null;

        Object string_literal9_tree = null;
        Object STRING10_tree = null;
        RewriteRuleTokenStream stream_45 = new RewriteRuleTokenStream(adaptor, "token 45");
        RewriteRuleTokenStream stream_STRING = new RewriteRuleTokenStream(adaptor, "token STRING");

        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:72:8: ( | 'mix' -> ^( PREFIX 'mix' ) | STRING -> ^( PREFIX STRING ) )
            int alt2 = 3;
            switch (input.LA(1)) {
                case 43: {
                    alt2 = 1;
                }
                break;
                case 45: {
                    alt2 = 2;
                }
                break;
                case STRING: {
                    alt2 = 3;
                }
                break;
                default:
                    if (state.backtracking > 0) {
                        state.failed = true;
                        return retval;
                    }
                    NoViableAltException nvae =
                            new NoViableAltException("", 2, 0, input);

                    throw nvae;
            }

            switch (alt2) {
                case 1:
                    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:73:3: 
                {
                    root_0 = (Object) adaptor.nil();

                }
                break;
                case 2:
                    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:73:5: 'mix'
                {
                    string_literal9 = (Token) match(input, 45, FOLLOW_45_in_prefix350);
                    if (state.failed) return retval;
                    if (state.backtracking == 0) stream_45.add(string_literal9);


                    // AST REWRITE
                    // elements: 45
                    // token labels: 
                    // rule labels: retval
                    // token list labels: 
                    // rule list labels: 
                    // wildcard labels: 
                    if (state.backtracking == 0) {
                        retval.tree = root_0;
                        RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "rule retval", retval != null ? retval.tree : null);

                        root_0 = (Object) adaptor.nil();
                        // 73:11: -> ^( PREFIX 'mix' )
                        {
                            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:73:14: ^( PREFIX 'mix' )
                            {
                                Object root_1 = (Object) adaptor.nil();
                                root_1 = (Object) adaptor.becomeRoot((Object) adaptor.create(PREFIX, "PREFIX"), root_1);

                                adaptor.addChild(root_1, stream_45.nextNode());

                                adaptor.addChild(root_0, root_1);
                            }

                        }

                        retval.tree = root_0;
                    }
                }
                break;
                case 3:
                    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:74:9: STRING
                {
                    STRING10 = (Token) match(input, STRING, FOLLOW_STRING_in_prefix372);
                    if (state.failed) return retval;
                    if (state.backtracking == 0) stream_STRING.add(STRING10);


                    // AST REWRITE
                    // elements: STRING
                    // token labels: 
                    // rule labels: retval
                    // token list labels: 
                    // rule list labels: 
                    // wildcard labels: 
                    if (state.backtracking == 0) {
                        retval.tree = root_0;
                        RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "rule retval", retval != null ? retval.tree : null);

                        root_0 = (Object) adaptor.nil();
                        // 74:16: -> ^( PREFIX STRING )
                        {
                            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:74:19: ^( PREFIX STRING )
                            {
                                Object root_1 = (Object) adaptor.nil();
                                root_1 = (Object) adaptor.becomeRoot((Object) adaptor.create(PREFIX, "PREFIX"), root_1);

                                adaptor.addChild(root_1, stream_STRING.nextNode());

                                adaptor.addChild(root_0, root_1);
                            }

                        }

                        retval.tree = root_0;
                    }
                }
                break;

            }
            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "prefix"

    public static class uri_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "uri"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:75:1: uri : STRING -> ^( URI STRING ) ;
    public final CndParser.uri_return uri() throws RecognitionException {
        CndParser.uri_return retval = new CndParser.uri_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token STRING11 = null;

        Object STRING11_tree = null;
        RewriteRuleTokenStream stream_STRING = new RewriteRuleTokenStream(adaptor, "token STRING");

        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:75:5: ( STRING -> ^( URI STRING ) )
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:75:7: STRING
            {
                STRING11 = (Token) match(input, STRING, FOLLOW_STRING_in_uri387);
                if (state.failed) return retval;
                if (state.backtracking == 0) stream_STRING.add(STRING11);


                // AST REWRITE
                // elements: STRING
                // token labels: 
                // rule labels: retval
                // token list labels: 
                // rule list labels: 
                // wildcard labels: 
                if (state.backtracking == 0) {
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "rule retval", retval != null ? retval.tree : null);

                    root_0 = (Object) adaptor.nil();
                    // 75:14: -> ^( URI STRING )
                    {
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:75:17: ^( URI STRING )
                        {
                            Object root_1 = (Object) adaptor.nil();
                            root_1 = (Object) adaptor.becomeRoot((Object) adaptor.create(URI, "URI"), root_1);

                            adaptor.addChild(root_1, stream_STRING.nextNode());

                            adaptor.addChild(root_0, root_1);
                        }

                    }

                    retval.tree = root_0;
                }
            }

            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "uri"

    public static class nodeTypeDefinition_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "nodeTypeDefinition"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:78:1: nodeTypeDefinition : nodeTypeName ( supertypes )? ( nodeTypeOptions )? ( propertyDefinition | childNodeDefinition )* -> ^( NODE nodeTypeName ^( PRIMARY_TYPE STRING[\"nt:nodeType\"] ) ( supertypes )? ( nodeTypeOptions )? ^( PROPERTY_DEFINITION ( propertyDefinition )* ) ^( CHILD_NODE_DEFINITION ( childNodeDefinition )* ) ) ;
    public final CndParser.nodeTypeDefinition_return nodeTypeDefinition() throws RecognitionException {
        CndParser.nodeTypeDefinition_return retval = new CndParser.nodeTypeDefinition_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        CndParser.nodeTypeName_return nodeTypeName12 = null;

        CndParser.supertypes_return supertypes13 = null;

        CndParser.nodeTypeOptions_return nodeTypeOptions14 = null;

        CndParser.propertyDefinition_return propertyDefinition15 = null;

        CndParser.childNodeDefinition_return childNodeDefinition16 = null;


        RewriteRuleSubtreeStream stream_supertypes = new RewriteRuleSubtreeStream(adaptor, "rule supertypes");
        RewriteRuleSubtreeStream stream_propertyDefinition = new RewriteRuleSubtreeStream(adaptor, "rule propertyDefinition");
        RewriteRuleSubtreeStream stream_childNodeDefinition = new RewriteRuleSubtreeStream(adaptor, "rule childNodeDefinition");
        RewriteRuleSubtreeStream stream_nodeTypeName = new RewriteRuleSubtreeStream(adaptor, "rule nodeTypeName");
        RewriteRuleSubtreeStream stream_nodeTypeOptions = new RewriteRuleSubtreeStream(adaptor, "rule nodeTypeOptions");
        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:78:20: ( nodeTypeName ( supertypes )? ( nodeTypeOptions )? ( propertyDefinition | childNodeDefinition )* -> ^( NODE nodeTypeName ^( PRIMARY_TYPE STRING[\"nt:nodeType\"] ) ( supertypes )? ( nodeTypeOptions )? ^( PROPERTY_DEFINITION ( propertyDefinition )* ) ^( CHILD_NODE_DEFINITION ( childNodeDefinition )* ) ) )
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:78:25: nodeTypeName ( supertypes )? ( nodeTypeOptions )? ( propertyDefinition | childNodeDefinition )*
            {
                pushFollow(FOLLOW_nodeTypeName_in_nodeTypeDefinition407);
                nodeTypeName12 = nodeTypeName();

                state._fsp--;
                if (state.failed) return retval;
                if (state.backtracking == 0) stream_nodeTypeName.add(nodeTypeName12.getTree());
                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:78:38: ( supertypes )?
                int alt3 = 2;
                int LA3_0 = input.LA(1);

                if ((LA3_0 == 44)) {
                    alt3 = 1;
                }
                switch (alt3) {
                    case 1:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:78:38: supertypes
                    {
                        pushFollow(FOLLOW_supertypes_in_nodeTypeDefinition409);
                        supertypes13 = supertypes();

                        state._fsp--;
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_supertypes.add(supertypes13.getTree());

                    }
                    break;

                }

                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:78:50: ( nodeTypeOptions )?
                int alt4 = 2;
                int LA4_0 = input.LA(1);

                if ((LA4_0 == 45 || (LA4_0 >= 48 && LA4_0 <= 59))) {
                    alt4 = 1;
                }
                switch (alt4) {
                    case 1:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:78:50: nodeTypeOptions
                    {
                        pushFollow(FOLLOW_nodeTypeOptions_in_nodeTypeDefinition412);
                        nodeTypeOptions14 = nodeTypeOptions();

                        state._fsp--;
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_nodeTypeOptions.add(nodeTypeOptions14.getTree());

                    }
                    break;

                }

                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:78:67: ( propertyDefinition | childNodeDefinition )*
                loop5:
                do {
                    int alt5 = 3;
                    int LA5_0 = input.LA(1);

                    if ((LA5_0 == 60)) {
                        alt5 = 1;
                    } else if ((LA5_0 == 100)) {
                        alt5 = 2;
                    }


                    switch (alt5) {
                        case 1:
                            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:78:69: propertyDefinition
                        {
                            pushFollow(FOLLOW_propertyDefinition_in_nodeTypeDefinition417);
                            propertyDefinition15 = propertyDefinition();

                            state._fsp--;
                            if (state.failed) return retval;
                            if (state.backtracking == 0) stream_propertyDefinition.add(propertyDefinition15.getTree());

                        }
                        break;
                        case 2:
                            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:78:90: childNodeDefinition
                        {
                            pushFollow(FOLLOW_childNodeDefinition_in_nodeTypeDefinition421);
                            childNodeDefinition16 = childNodeDefinition();

                            state._fsp--;
                            if (state.failed) return retval;
                            if (state.backtracking == 0)
                                stream_childNodeDefinition.add(childNodeDefinition16.getTree());

                        }
                        break;

                        default:
                            break loop5;
                    }
                } while (true);


                // AST REWRITE
                // elements: nodeTypeName, supertypes, nodeTypeOptions, propertyDefinition, childNodeDefinition
                // token labels: 
                // rule labels: retval
                // token list labels: 
                // rule list labels: 
                // wildcard labels: 
                if (state.backtracking == 0) {
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "rule retval", retval != null ? retval.tree : null);

                    root_0 = (Object) adaptor.nil();
                    // 79:5: -> ^( NODE nodeTypeName ^( PRIMARY_TYPE STRING[\"nt:nodeType\"] ) ( supertypes )? ( nodeTypeOptions )? ^( PROPERTY_DEFINITION ( propertyDefinition )* ) ^( CHILD_NODE_DEFINITION ( childNodeDefinition )* ) )
                    {
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:79:8: ^( NODE nodeTypeName ^( PRIMARY_TYPE STRING[\"nt:nodeType\"] ) ( supertypes )? ( nodeTypeOptions )? ^( PROPERTY_DEFINITION ( propertyDefinition )* ) ^( CHILD_NODE_DEFINITION ( childNodeDefinition )* ) )
                        {
                            Object root_1 = (Object) adaptor.nil();
                            root_1 = (Object) adaptor.becomeRoot((Object) adaptor.create(NODE, "NODE"), root_1);

                            adaptor.addChild(root_1, stream_nodeTypeName.nextTree());
                            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:79:28: ^( PRIMARY_TYPE STRING[\"nt:nodeType\"] )
                            {
                                Object root_2 = (Object) adaptor.nil();
                                root_2 = (Object) adaptor.becomeRoot((Object) adaptor.create(PRIMARY_TYPE, "PRIMARY_TYPE"), root_2);

                                adaptor.addChild(root_2, (Object) adaptor.create(STRING, "nt:nodeType"));

                                adaptor.addChild(root_1, root_2);
                            }
                            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:79:66: ( supertypes )?
                            if (stream_supertypes.hasNext()) {
                                adaptor.addChild(root_1, stream_supertypes.nextTree());

                            }
                            stream_supertypes.reset();
                            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:79:78: ( nodeTypeOptions )?
                            if (stream_nodeTypeOptions.hasNext()) {
                                adaptor.addChild(root_1, stream_nodeTypeOptions.nextTree());

                            }
                            stream_nodeTypeOptions.reset();
                            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:79:95: ^( PROPERTY_DEFINITION ( propertyDefinition )* )
                            {
                                Object root_2 = (Object) adaptor.nil();
                                root_2 = (Object) adaptor.becomeRoot((Object) adaptor.create(PROPERTY_DEFINITION, "PROPERTY_DEFINITION"), root_2);

                                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:79:117: ( propertyDefinition )*
                                while (stream_propertyDefinition.hasNext()) {
                                    adaptor.addChild(root_2, stream_propertyDefinition.nextTree());

                                }
                                stream_propertyDefinition.reset();

                                adaptor.addChild(root_1, root_2);
                            }
                            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:79:138: ^( CHILD_NODE_DEFINITION ( childNodeDefinition )* )
                            {
                                Object root_2 = (Object) adaptor.nil();
                                root_2 = (Object) adaptor.becomeRoot((Object) adaptor.create(CHILD_NODE_DEFINITION, "CHILD_NODE_DEFINITION"), root_2);

                                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:79:162: ( childNodeDefinition )*
                                while (stream_childNodeDefinition.hasNext()) {
                                    adaptor.addChild(root_2, stream_childNodeDefinition.nextTree());

                                }
                                stream_childNodeDefinition.reset();

                                adaptor.addChild(root_1, root_2);
                            }

                            adaptor.addChild(root_0, root_1);
                        }

                    }

                    retval.tree = root_0;
                }
            }

            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "nodeTypeDefinition"

    public static class nodeTypeName_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "nodeTypeName"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:80:1: nodeTypeName : '[' STRING ']' -> ^( NAME STRING ) ;
    public final CndParser.nodeTypeName_return nodeTypeName() throws RecognitionException {
        CndParser.nodeTypeName_return retval = new CndParser.nodeTypeName_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token char_literal17 = null;
        Token STRING18 = null;
        Token char_literal19 = null;

        Object char_literal17_tree = null;
        Object STRING18_tree = null;
        Object char_literal19_tree = null;
        RewriteRuleTokenStream stream_47 = new RewriteRuleTokenStream(adaptor, "token 47");
        RewriteRuleTokenStream stream_46 = new RewriteRuleTokenStream(adaptor, "token 46");
        RewriteRuleTokenStream stream_STRING = new RewriteRuleTokenStream(adaptor, "token STRING");

        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:80:14: ( '[' STRING ']' -> ^( NAME STRING ) )
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:80:16: '[' STRING ']'
            {
                char_literal17 = (Token) match(input, 46, FOLLOW_46_in_nodeTypeName470);
                if (state.failed) return retval;
                if (state.backtracking == 0) stream_46.add(char_literal17);

                STRING18 = (Token) match(input, STRING, FOLLOW_STRING_in_nodeTypeName472);
                if (state.failed) return retval;
                if (state.backtracking == 0) stream_STRING.add(STRING18);

                char_literal19 = (Token) match(input, 47, FOLLOW_47_in_nodeTypeName474);
                if (state.failed) return retval;
                if (state.backtracking == 0) stream_47.add(char_literal19);


                // AST REWRITE
                // elements: STRING
                // token labels: 
                // rule labels: retval
                // token list labels: 
                // rule list labels: 
                // wildcard labels: 
                if (state.backtracking == 0) {
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "rule retval", retval != null ? retval.tree : null);

                    root_0 = (Object) adaptor.nil();
                    // 80:31: -> ^( NAME STRING )
                    {
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:80:34: ^( NAME STRING )
                        {
                            Object root_1 = (Object) adaptor.nil();
                            root_1 = (Object) adaptor.becomeRoot((Object) adaptor.create(NAME, "NAME"), root_1);

                            adaptor.addChild(root_1, stream_STRING.nextNode());

                            adaptor.addChild(root_0, root_1);
                        }

                    }

                    retval.tree = root_0;
                }
            }

            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "nodeTypeName"

    public static class supertypes_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "supertypes"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:81:1: supertypes : '>' stringList -> ^( SUPERTYPES stringList ) ;
    public final CndParser.supertypes_return supertypes() throws RecognitionException {
        CndParser.supertypes_return retval = new CndParser.supertypes_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token char_literal20 = null;
        CndParser.stringList_return stringList21 = null;


        Object char_literal20_tree = null;
        RewriteRuleTokenStream stream_44 = new RewriteRuleTokenStream(adaptor, "token 44");
        RewriteRuleSubtreeStream stream_stringList = new RewriteRuleSubtreeStream(adaptor, "rule stringList");
        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:81:12: ( '>' stringList -> ^( SUPERTYPES stringList ) )
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:81:14: '>' stringList
            {
                char_literal20 = (Token) match(input, 44, FOLLOW_44_in_supertypes489);
                if (state.failed) return retval;
                if (state.backtracking == 0) stream_44.add(char_literal20);

                pushFollow(FOLLOW_stringList_in_supertypes491);
                stringList21 = stringList();

                state._fsp--;
                if (state.failed) return retval;
                if (state.backtracking == 0) stream_stringList.add(stringList21.getTree());


                // AST REWRITE
                // elements: stringList
                // token labels: 
                // rule labels: retval
                // token list labels: 
                // rule list labels: 
                // wildcard labels: 
                if (state.backtracking == 0) {
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "rule retval", retval != null ? retval.tree : null);

                    root_0 = (Object) adaptor.nil();
                    // 81:29: -> ^( SUPERTYPES stringList )
                    {
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:81:32: ^( SUPERTYPES stringList )
                        {
                            Object root_1 = (Object) adaptor.nil();
                            root_1 = (Object) adaptor.becomeRoot((Object) adaptor.create(SUPERTYPES, "SUPERTYPES"), root_1);

                            adaptor.addChild(root_1, stream_stringList.nextTree());

                            adaptor.addChild(root_0, root_1);
                        }

                    }

                    retval.tree = root_0;
                }
            }

            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "supertypes"

    public static class nodeTypeOptions_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "nodeTypeOptions"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:82:1: nodeTypeOptions : ( nodeTypeOption )+ ;
    public final CndParser.nodeTypeOptions_return nodeTypeOptions() throws RecognitionException {
        CndParser.nodeTypeOptions_return retval = new CndParser.nodeTypeOptions_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        CndParser.nodeTypeOption_return nodeTypeOption22 = null;


        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:82:17: ( ( nodeTypeOption )+ )
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:82:21: ( nodeTypeOption )+
            {
                root_0 = (Object) adaptor.nil();

                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:82:21: ( nodeTypeOption )+
                int cnt6 = 0;
                loop6:
                do {
                    int alt6 = 2;
                    int LA6_0 = input.LA(1);

                    if ((LA6_0 == 45 || (LA6_0 >= 48 && LA6_0 <= 59))) {
                        alt6 = 1;
                    }


                    switch (alt6) {
                        case 1:
                            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:82:21: nodeTypeOption
                        {
                            pushFollow(FOLLOW_nodeTypeOption_in_nodeTypeOptions508);
                            nodeTypeOption22 = nodeTypeOption();

                            state._fsp--;
                            if (state.failed) return retval;
                            if (state.backtracking == 0) adaptor.addChild(root_0, nodeTypeOption22.getTree());

                        }
                        break;

                        default:
                            if (cnt6 >= 1) break loop6;
                            if (state.backtracking > 0) {
                                state.failed = true;
                                return retval;
                            }
                            EarlyExitException eee =
                                    new EarlyExitException(6, input);
                            throw eee;
                    }
                    cnt6++;
                } while (true);


            }

            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "nodeTypeOptions"

    public static class nodeTypeOption_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "nodeTypeOption"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:83:1: nodeTypeOption : ( orderable | mixin | isAbstract | noQuery | primaryItem );
    public final CndParser.nodeTypeOption_return nodeTypeOption() throws RecognitionException {
        CndParser.nodeTypeOption_return retval = new CndParser.nodeTypeOption_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        CndParser.orderable_return orderable23 = null;

        CndParser.mixin_return mixin24 = null;

        CndParser.isAbstract_return isAbstract25 = null;

        CndParser.noQuery_return noQuery26 = null;

        CndParser.primaryItem_return primaryItem27 = null;


        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:83:16: ( orderable | mixin | isAbstract | noQuery | primaryItem )
            int alt7 = 5;
            switch (input.LA(1)) {
                case 48:
                case 49:
                case 50: {
                    alt7 = 1;
                }
                break;
                case 45:
                case 51:
                case 52: {
                    alt7 = 2;
                }
                break;
                case 53:
                case 54:
                case 55: {
                    alt7 = 3;
                }
                break;
                case 56:
                case 57: {
                    alt7 = 4;
                }
                break;
                case 58:
                case 59: {
                    alt7 = 5;
                }
                break;
                default:
                    if (state.backtracking > 0) {
                        state.failed = true;
                        return retval;
                    }
                    NoViableAltException nvae =
                            new NoViableAltException("", 7, 0, input);

                    throw nvae;
            }

            switch (alt7) {
                case 1:
                    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:83:21: orderable
                {
                    root_0 = (Object) adaptor.nil();

                    pushFollow(FOLLOW_orderable_in_nodeTypeOption519);
                    orderable23 = orderable();

                    state._fsp--;
                    if (state.failed) return retval;
                    if (state.backtracking == 0) adaptor.addChild(root_0, orderable23.getTree());

                }
                break;
                case 2:
                    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:83:33: mixin
                {
                    root_0 = (Object) adaptor.nil();

                    pushFollow(FOLLOW_mixin_in_nodeTypeOption523);
                    mixin24 = mixin();

                    state._fsp--;
                    if (state.failed) return retval;
                    if (state.backtracking == 0) adaptor.addChild(root_0, mixin24.getTree());

                }
                break;
                case 3:
                    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:83:41: isAbstract
                {
                    root_0 = (Object) adaptor.nil();

                    pushFollow(FOLLOW_isAbstract_in_nodeTypeOption527);
                    isAbstract25 = isAbstract();

                    state._fsp--;
                    if (state.failed) return retval;
                    if (state.backtracking == 0) adaptor.addChild(root_0, isAbstract25.getTree());

                }
                break;
                case 4:
                    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:83:54: noQuery
                {
                    root_0 = (Object) adaptor.nil();

                    pushFollow(FOLLOW_noQuery_in_nodeTypeOption531);
                    noQuery26 = noQuery();

                    state._fsp--;
                    if (state.failed) return retval;
                    if (state.backtracking == 0) adaptor.addChild(root_0, noQuery26.getTree());

                }
                break;
                case 5:
                    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:83:64: primaryItem
                {
                    root_0 = (Object) adaptor.nil();

                    pushFollow(FOLLOW_primaryItem_in_nodeTypeOption535);
                    primaryItem27 = primaryItem();

                    state._fsp--;
                    if (state.failed) return retval;
                    if (state.backtracking == 0) adaptor.addChild(root_0, primaryItem27.getTree());

                }
                break;

            }
            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "nodeTypeOption"

    public static class orderable_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "orderable"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:84:1: orderable : ( 'o' | 'ord' | 'orderable' ) -> ^( HAS_ORDERABLE_CHILD_NODES STRING[\"true\"] ) ;
    public final CndParser.orderable_return orderable() throws RecognitionException {
        CndParser.orderable_return retval = new CndParser.orderable_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token char_literal28 = null;
        Token string_literal29 = null;
        Token string_literal30 = null;

        Object char_literal28_tree = null;
        Object string_literal29_tree = null;
        Object string_literal30_tree = null;
        RewriteRuleTokenStream stream_49 = new RewriteRuleTokenStream(adaptor, "token 49");
        RewriteRuleTokenStream stream_48 = new RewriteRuleTokenStream(adaptor, "token 48");
        RewriteRuleTokenStream stream_50 = new RewriteRuleTokenStream(adaptor, "token 50");

        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:84:11: ( ( 'o' | 'ord' | 'orderable' ) -> ^( HAS_ORDERABLE_CHILD_NODES STRING[\"true\"] ) )
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:84:13: ( 'o' | 'ord' | 'orderable' )
            {
                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:84:13: ( 'o' | 'ord' | 'orderable' )
                int alt8 = 3;
                switch (input.LA(1)) {
                    case 48: {
                        alt8 = 1;
                    }
                    break;
                    case 49: {
                        alt8 = 2;
                    }
                    break;
                    case 50: {
                        alt8 = 3;
                    }
                    break;
                    default:
                        if (state.backtracking > 0) {
                            state.failed = true;
                            return retval;
                        }
                        NoViableAltException nvae =
                                new NoViableAltException("", 8, 0, input);

                        throw nvae;
                }

                switch (alt8) {
                    case 1:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:84:14: 'o'
                    {
                        char_literal28 = (Token) match(input, 48, FOLLOW_48_in_orderable544);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_48.add(char_literal28);


                    }
                    break;
                    case 2:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:84:18: 'ord'
                    {
                        string_literal29 = (Token) match(input, 49, FOLLOW_49_in_orderable546);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_49.add(string_literal29);


                    }
                    break;
                    case 3:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:84:24: 'orderable'
                    {
                        string_literal30 = (Token) match(input, 50, FOLLOW_50_in_orderable548);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_50.add(string_literal30);


                    }
                    break;

                }


                // AST REWRITE
                // elements: 
                // token labels: 
                // rule labels: retval
                // token list labels: 
                // rule list labels: 
                // wildcard labels: 
                if (state.backtracking == 0) {
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "rule retval", retval != null ? retval.tree : null);

                    root_0 = (Object) adaptor.nil();
                    // 84:37: -> ^( HAS_ORDERABLE_CHILD_NODES STRING[\"true\"] )
                    {
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:84:40: ^( HAS_ORDERABLE_CHILD_NODES STRING[\"true\"] )
                        {
                            Object root_1 = (Object) adaptor.nil();
                            root_1 = (Object) adaptor.becomeRoot((Object) adaptor.create(HAS_ORDERABLE_CHILD_NODES, "HAS_ORDERABLE_CHILD_NODES"), root_1);

                            adaptor.addChild(root_1, (Object) adaptor.create(STRING, "true"));

                            adaptor.addChild(root_0, root_1);
                        }

                    }

                    retval.tree = root_0;
                }
            }

            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "orderable"

    public static class mixin_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "mixin"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:85:1: mixin : ( 'm' | 'mix' | 'mixin' ) -> ^( IS_MIXIN STRING[\"true\"] ) ;
    public final CndParser.mixin_return mixin() throws RecognitionException {
        CndParser.mixin_return retval = new CndParser.mixin_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token char_literal31 = null;
        Token string_literal32 = null;
        Token string_literal33 = null;

        Object char_literal31_tree = null;
        Object string_literal32_tree = null;
        Object string_literal33_tree = null;
        RewriteRuleTokenStream stream_45 = new RewriteRuleTokenStream(adaptor, "token 45");
        RewriteRuleTokenStream stream_51 = new RewriteRuleTokenStream(adaptor, "token 51");
        RewriteRuleTokenStream stream_52 = new RewriteRuleTokenStream(adaptor, "token 52");

        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:85:7: ( ( 'm' | 'mix' | 'mixin' ) -> ^( IS_MIXIN STRING[\"true\"] ) )
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:85:9: ( 'm' | 'mix' | 'mixin' )
            {
                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:85:9: ( 'm' | 'mix' | 'mixin' )
                int alt9 = 3;
                switch (input.LA(1)) {
                    case 51: {
                        alt9 = 1;
                    }
                    break;
                    case 45: {
                        alt9 = 2;
                    }
                    break;
                    case 52: {
                        alt9 = 3;
                    }
                    break;
                    default:
                        if (state.backtracking > 0) {
                            state.failed = true;
                            return retval;
                        }
                        NoViableAltException nvae =
                                new NoViableAltException("", 9, 0, input);

                        throw nvae;
                }

                switch (alt9) {
                    case 1:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:85:10: 'm'
                    {
                        char_literal31 = (Token) match(input, 51, FOLLOW_51_in_mixin566);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_51.add(char_literal31);


                    }
                    break;
                    case 2:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:85:16: 'mix'
                    {
                        string_literal32 = (Token) match(input, 45, FOLLOW_45_in_mixin570);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_45.add(string_literal32);


                    }
                    break;
                    case 3:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:85:24: 'mixin'
                    {
                        string_literal33 = (Token) match(input, 52, FOLLOW_52_in_mixin574);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_52.add(string_literal33);


                    }
                    break;

                }


                // AST REWRITE
                // elements: 
                // token labels: 
                // rule labels: retval
                // token list labels: 
                // rule list labels: 
                // wildcard labels: 
                if (state.backtracking == 0) {
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "rule retval", retval != null ? retval.tree : null);

                    root_0 = (Object) adaptor.nil();
                    // 85:33: -> ^( IS_MIXIN STRING[\"true\"] )
                    {
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:85:36: ^( IS_MIXIN STRING[\"true\"] )
                        {
                            Object root_1 = (Object) adaptor.nil();
                            root_1 = (Object) adaptor.becomeRoot((Object) adaptor.create(IS_MIXIN, "IS_MIXIN"), root_1);

                            adaptor.addChild(root_1, (Object) adaptor.create(STRING, "true"));

                            adaptor.addChild(root_0, root_1);
                        }

                    }

                    retval.tree = root_0;
                }
            }

            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "mixin"

    public static class isAbstract_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "isAbstract"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:86:1: isAbstract : ( 'a' | 'abs' | 'abstract' ) -> ^( IS_ABSTRACT STRING[\"true\"] ) ;
    public final CndParser.isAbstract_return isAbstract() throws RecognitionException {
        CndParser.isAbstract_return retval = new CndParser.isAbstract_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token char_literal34 = null;
        Token string_literal35 = null;
        Token string_literal36 = null;

        Object char_literal34_tree = null;
        Object string_literal35_tree = null;
        Object string_literal36_tree = null;
        RewriteRuleTokenStream stream_55 = new RewriteRuleTokenStream(adaptor, "token 55");
        RewriteRuleTokenStream stream_53 = new RewriteRuleTokenStream(adaptor, "token 53");
        RewriteRuleTokenStream stream_54 = new RewriteRuleTokenStream(adaptor, "token 54");

        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:86:12: ( ( 'a' | 'abs' | 'abstract' ) -> ^( IS_ABSTRACT STRING[\"true\"] ) )
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:86:17: ( 'a' | 'abs' | 'abstract' )
            {
                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:86:17: ( 'a' | 'abs' | 'abstract' )
                int alt10 = 3;
                switch (input.LA(1)) {
                    case 53: {
                        alt10 = 1;
                    }
                    break;
                    case 54: {
                        alt10 = 2;
                    }
                    break;
                    case 55: {
                        alt10 = 3;
                    }
                    break;
                    default:
                        if (state.backtracking > 0) {
                            state.failed = true;
                            return retval;
                        }
                        NoViableAltException nvae =
                                new NoViableAltException("", 10, 0, input);

                        throw nvae;
                }

                switch (alt10) {
                    case 1:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:86:18: 'a'
                    {
                        char_literal34 = (Token) match(input, 53, FOLLOW_53_in_isAbstract595);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_53.add(char_literal34);


                    }
                    break;
                    case 2:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:86:22: 'abs'
                    {
                        string_literal35 = (Token) match(input, 54, FOLLOW_54_in_isAbstract597);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_54.add(string_literal35);


                    }
                    break;
                    case 3:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:86:28: 'abstract'
                    {
                        string_literal36 = (Token) match(input, 55, FOLLOW_55_in_isAbstract599);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_55.add(string_literal36);


                    }
                    break;

                }


                // AST REWRITE
                // elements: 
                // token labels: 
                // rule labels: retval
                // token list labels: 
                // rule list labels: 
                // wildcard labels: 
                if (state.backtracking == 0) {
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "rule retval", retval != null ? retval.tree : null);

                    root_0 = (Object) adaptor.nil();
                    // 86:40: -> ^( IS_ABSTRACT STRING[\"true\"] )
                    {
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:86:43: ^( IS_ABSTRACT STRING[\"true\"] )
                        {
                            Object root_1 = (Object) adaptor.nil();
                            root_1 = (Object) adaptor.becomeRoot((Object) adaptor.create(IS_ABSTRACT, "IS_ABSTRACT"), root_1);

                            adaptor.addChild(root_1, (Object) adaptor.create(STRING, "true"));

                            adaptor.addChild(root_0, root_1);
                        }

                    }

                    retval.tree = root_0;
                }
            }

            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "isAbstract"

    public static class noQuery_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "noQuery"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:87:1: noQuery : ( 'nq' | 'noquery' ) -> ^( IS_QUERYABLE STRING[\"false\"] ) ;
    public final CndParser.noQuery_return noQuery() throws RecognitionException {
        CndParser.noQuery_return retval = new CndParser.noQuery_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token string_literal37 = null;
        Token string_literal38 = null;

        Object string_literal37_tree = null;
        Object string_literal38_tree = null;
        RewriteRuleTokenStream stream_57 = new RewriteRuleTokenStream(adaptor, "token 57");
        RewriteRuleTokenStream stream_56 = new RewriteRuleTokenStream(adaptor, "token 56");

        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:87:9: ( ( 'nq' | 'noquery' ) -> ^( IS_QUERYABLE STRING[\"false\"] ) )
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:87:13: ( 'nq' | 'noquery' )
            {
                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:87:13: ( 'nq' | 'noquery' )
                int alt11 = 2;
                int LA11_0 = input.LA(1);

                if ((LA11_0 == 56)) {
                    alt11 = 1;
                } else if ((LA11_0 == 57)) {
                    alt11 = 2;
                } else {
                    if (state.backtracking > 0) {
                        state.failed = true;
                        return retval;
                    }
                    NoViableAltException nvae =
                            new NoViableAltException("", 11, 0, input);

                    throw nvae;
                }
                switch (alt11) {
                    case 1:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:87:14: 'nq'
                    {
                        string_literal37 = (Token) match(input, 56, FOLLOW_56_in_noQuery619);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_56.add(string_literal37);


                    }
                    break;
                    case 2:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:87:19: 'noquery'
                    {
                        string_literal38 = (Token) match(input, 57, FOLLOW_57_in_noQuery621);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_57.add(string_literal38);


                    }
                    break;

                }


                // AST REWRITE
                // elements: 
                // token labels: 
                // rule labels: retval
                // token list labels: 
                // rule list labels: 
                // wildcard labels: 
                if (state.backtracking == 0) {
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "rule retval", retval != null ? retval.tree : null);

                    root_0 = (Object) adaptor.nil();
                    // 87:30: -> ^( IS_QUERYABLE STRING[\"false\"] )
                    {
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:87:33: ^( IS_QUERYABLE STRING[\"false\"] )
                        {
                            Object root_1 = (Object) adaptor.nil();
                            root_1 = (Object) adaptor.becomeRoot((Object) adaptor.create(IS_QUERYABLE, "IS_QUERYABLE"), root_1);

                            adaptor.addChild(root_1, (Object) adaptor.create(STRING, "false"));

                            adaptor.addChild(root_0, root_1);
                        }

                    }

                    retval.tree = root_0;
                }
            }

            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "noQuery"

    public static class primaryItem_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "primaryItem"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:88:1: primaryItem : ( 'primaryitem' | '!' ) STRING -> ^( PRIMARY_ITEM_NAME STRING ) ;
    public final CndParser.primaryItem_return primaryItem() throws RecognitionException {
        CndParser.primaryItem_return retval = new CndParser.primaryItem_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token string_literal39 = null;
        Token char_literal40 = null;
        Token STRING41 = null;

        Object string_literal39_tree = null;
        Object char_literal40_tree = null;
        Object STRING41_tree = null;
        RewriteRuleTokenStream stream_59 = new RewriteRuleTokenStream(adaptor, "token 59");
        RewriteRuleTokenStream stream_58 = new RewriteRuleTokenStream(adaptor, "token 58");
        RewriteRuleTokenStream stream_STRING = new RewriteRuleTokenStream(adaptor, "token STRING");

        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:88:13: ( ( 'primaryitem' | '!' ) STRING -> ^( PRIMARY_ITEM_NAME STRING ) )
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:88:15: ( 'primaryitem' | '!' ) STRING
            {
                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:88:15: ( 'primaryitem' | '!' )
                int alt12 = 2;
                int LA12_0 = input.LA(1);

                if ((LA12_0 == 58)) {
                    alt12 = 1;
                } else if ((LA12_0 == 59)) {
                    alt12 = 2;
                } else {
                    if (state.backtracking > 0) {
                        state.failed = true;
                        return retval;
                    }
                    NoViableAltException nvae =
                            new NoViableAltException("", 12, 0, input);

                    throw nvae;
                }
                switch (alt12) {
                    case 1:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:88:16: 'primaryitem'
                    {
                        string_literal39 = (Token) match(input, 58, FOLLOW_58_in_primaryItem639);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_58.add(string_literal39);


                    }
                    break;
                    case 2:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:88:30: '!'
                    {
                        char_literal40 = (Token) match(input, 59, FOLLOW_59_in_primaryItem641);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_59.add(char_literal40);


                    }
                    break;

                }

                STRING41 = (Token) match(input, STRING, FOLLOW_STRING_in_primaryItem646);
                if (state.failed) return retval;
                if (state.backtracking == 0) stream_STRING.add(STRING41);


                // AST REWRITE
                // elements: STRING
                // token labels: 
                // rule labels: retval
                // token list labels: 
                // rule list labels: 
                // wildcard labels: 
                if (state.backtracking == 0) {
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "rule retval", retval != null ? retval.tree : null);

                    root_0 = (Object) adaptor.nil();
                    // 88:44: -> ^( PRIMARY_ITEM_NAME STRING )
                    {
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:88:47: ^( PRIMARY_ITEM_NAME STRING )
                        {
                            Object root_1 = (Object) adaptor.nil();
                            root_1 = (Object) adaptor.becomeRoot((Object) adaptor.create(PRIMARY_ITEM_NAME, "PRIMARY_ITEM_NAME"), root_1);

                            adaptor.addChild(root_1, stream_STRING.nextNode());

                            adaptor.addChild(root_0, root_1);
                        }

                    }

                    retval.tree = root_0;
                }
            }

            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "primaryItem"

    public static class propertyDefinition_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "propertyDefinition"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:91:1: propertyDefinition : propertyName ( propertyType )? ( defaultValues )? ( propertyAttributes | valueConstraints )* -> ^( NODE propertyName ^( PRIMARY_TYPE STRING[\"nt:propertyDefinition\"] ) ( propertyType )? ( defaultValues )? ( propertyAttributes )* ( valueConstraints )* ) ;
    public final CndParser.propertyDefinition_return propertyDefinition() throws RecognitionException {
        CndParser.propertyDefinition_return retval = new CndParser.propertyDefinition_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        CndParser.propertyName_return propertyName42 = null;

        CndParser.propertyType_return propertyType43 = null;

        CndParser.defaultValues_return defaultValues44 = null;

        CndParser.propertyAttributes_return propertyAttributes45 = null;

        CndParser.valueConstraints_return valueConstraints46 = null;


        RewriteRuleSubtreeStream stream_propertyName = new RewriteRuleSubtreeStream(adaptor, "rule propertyName");
        RewriteRuleSubtreeStream stream_propertyType = new RewriteRuleSubtreeStream(adaptor, "rule propertyType");
        RewriteRuleSubtreeStream stream_defaultValues = new RewriteRuleSubtreeStream(adaptor, "rule defaultValues");
        RewriteRuleSubtreeStream stream_propertyAttributes = new RewriteRuleSubtreeStream(adaptor, "rule propertyAttributes");
        RewriteRuleSubtreeStream stream_valueConstraints = new RewriteRuleSubtreeStream(adaptor, "rule valueConstraints");
        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:91:20: ( propertyName ( propertyType )? ( defaultValues )? ( propertyAttributes | valueConstraints )* -> ^( NODE propertyName ^( PRIMARY_TYPE STRING[\"nt:propertyDefinition\"] ) ( propertyType )? ( defaultValues )? ( propertyAttributes )* ( valueConstraints )* ) )
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:91:22: propertyName ( propertyType )? ( defaultValues )? ( propertyAttributes | valueConstraints )*
            {
                pushFollow(FOLLOW_propertyName_in_propertyDefinition663);
                propertyName42 = propertyName();

                state._fsp--;
                if (state.failed) return retval;
                if (state.backtracking == 0) stream_propertyName.add(propertyName42.getTree());
                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:91:35: ( propertyType )?
                int alt13 = 2;
                int LA13_0 = input.LA(1);

                if ((LA13_0 == 62)) {
                    alt13 = 1;
                }
                switch (alt13) {
                    case 1:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:91:35: propertyType
                    {
                        pushFollow(FOLLOW_propertyType_in_propertyDefinition665);
                        propertyType43 = propertyType();

                        state._fsp--;
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_propertyType.add(propertyType43.getTree());

                    }
                    break;

                }

                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:91:49: ( defaultValues )?
                int alt14 = 2;
                int LA14_0 = input.LA(1);

                if ((LA14_0 == 43)) {
                    alt14 = 1;
                }
                switch (alt14) {
                    case 1:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:91:49: defaultValues
                    {
                        pushFollow(FOLLOW_defaultValues_in_propertyDefinition668);
                        defaultValues44 = defaultValues();

                        state._fsp--;
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_defaultValues.add(defaultValues44.getTree());

                    }
                    break;

                }

                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:91:64: ( propertyAttributes | valueConstraints )*
                loop15:
                do {
                    int alt15 = 3;
                    int LA15_0 = input.LA(1);

                    if ((LA15_0 == 42)) {
                        int LA15_2 = input.LA(2);

                        if ((LA15_2 == STRING)) {
                            int LA15_4 = input.LA(3);

                            if ((LA15_4 == EOF || LA15_4 == 42 || LA15_4 == 46 || LA15_4 == 51 || LA15_4 == 53 || (LA15_4 >= 59 && LA15_4 <= 61) || (LA15_4 >= 77 && LA15_4 <= 100) || LA15_4 == 102)) {
                                alt15 = 2;
                            }


                        }


                    } else if ((LA15_0 == 51 || LA15_0 == 53 || LA15_0 == 59 || LA15_0 == 61 || (LA15_0 >= 77 && LA15_0 <= 99))) {
                        alt15 = 1;
                    }


                    switch (alt15) {
                        case 1:
                            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:91:66: propertyAttributes
                        {
                            pushFollow(FOLLOW_propertyAttributes_in_propertyDefinition673);
                            propertyAttributes45 = propertyAttributes();

                            state._fsp--;
                            if (state.failed) return retval;
                            if (state.backtracking == 0) stream_propertyAttributes.add(propertyAttributes45.getTree());

                        }
                        break;
                        case 2:
                            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:91:87: valueConstraints
                        {
                            pushFollow(FOLLOW_valueConstraints_in_propertyDefinition677);
                            valueConstraints46 = valueConstraints();

                            state._fsp--;
                            if (state.failed) return retval;
                            if (state.backtracking == 0) stream_valueConstraints.add(valueConstraints46.getTree());

                        }
                        break;

                        default:
                            break loop15;
                    }
                } while (true);


                // AST REWRITE
                // elements: propertyAttributes, propertyName, valueConstraints, defaultValues, propertyType
                // token labels: 
                // rule labels: retval
                // token list labels: 
                // rule list labels: 
                // wildcard labels: 
                if (state.backtracking == 0) {
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "rule retval", retval != null ? retval.tree : null);

                    root_0 = (Object) adaptor.nil();
                    // 92:5: -> ^( NODE propertyName ^( PRIMARY_TYPE STRING[\"nt:propertyDefinition\"] ) ( propertyType )? ( defaultValues )? ( propertyAttributes )* ( valueConstraints )* )
                    {
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:92:8: ^( NODE propertyName ^( PRIMARY_TYPE STRING[\"nt:propertyDefinition\"] ) ( propertyType )? ( defaultValues )? ( propertyAttributes )* ( valueConstraints )* )
                        {
                            Object root_1 = (Object) adaptor.nil();
                            root_1 = (Object) adaptor.becomeRoot((Object) adaptor.create(NODE, "NODE"), root_1);

                            adaptor.addChild(root_1, stream_propertyName.nextTree());
                            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:92:28: ^( PRIMARY_TYPE STRING[\"nt:propertyDefinition\"] )
                            {
                                Object root_2 = (Object) adaptor.nil();
                                root_2 = (Object) adaptor.becomeRoot((Object) adaptor.create(PRIMARY_TYPE, "PRIMARY_TYPE"), root_2);

                                adaptor.addChild(root_2, (Object) adaptor.create(STRING, "nt:propertyDefinition"));

                                adaptor.addChild(root_1, root_2);
                            }
                            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:92:76: ( propertyType )?
                            if (stream_propertyType.hasNext()) {
                                adaptor.addChild(root_1, stream_propertyType.nextTree());

                            }
                            stream_propertyType.reset();
                            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:92:90: ( defaultValues )?
                            if (stream_defaultValues.hasNext()) {
                                adaptor.addChild(root_1, stream_defaultValues.nextTree());

                            }
                            stream_defaultValues.reset();
                            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:92:105: ( propertyAttributes )*
                            while (stream_propertyAttributes.hasNext()) {
                                adaptor.addChild(root_1, stream_propertyAttributes.nextTree());

                            }
                            stream_propertyAttributes.reset();
                            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:92:125: ( valueConstraints )*
                            while (stream_valueConstraints.hasNext()) {
                                adaptor.addChild(root_1, stream_valueConstraints.nextTree());

                            }
                            stream_valueConstraints.reset();

                            adaptor.addChild(root_0, root_1);
                        }

                    }

                    retval.tree = root_0;
                }
            }

            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "propertyDefinition"

    public static class propertyName_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "propertyName"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:93:1: propertyName : '-' ( '*' -> ^( NAME '*' ) | STRING -> ^( NAME STRING ) ) ;
    public final CndParser.propertyName_return propertyName() throws RecognitionException {
        CndParser.propertyName_return retval = new CndParser.propertyName_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token char_literal47 = null;
        Token char_literal48 = null;
        Token STRING49 = null;

        Object char_literal47_tree = null;
        Object char_literal48_tree = null;
        Object STRING49_tree = null;
        RewriteRuleTokenStream stream_60 = new RewriteRuleTokenStream(adaptor, "token 60");
        RewriteRuleTokenStream stream_61 = new RewriteRuleTokenStream(adaptor, "token 61");
        RewriteRuleTokenStream stream_STRING = new RewriteRuleTokenStream(adaptor, "token STRING");

        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:93:14: ( '-' ( '*' -> ^( NAME '*' ) | STRING -> ^( NAME STRING ) ) )
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:93:16: '-' ( '*' -> ^( NAME '*' ) | STRING -> ^( NAME STRING ) )
            {
                char_literal47 = (Token) match(input, 60, FOLLOW_60_in_propertyName718);
                if (state.failed) return retval;
                if (state.backtracking == 0) stream_60.add(char_literal47);

                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:93:20: ( '*' -> ^( NAME '*' ) | STRING -> ^( NAME STRING ) )
                int alt16 = 2;
                int LA16_0 = input.LA(1);

                if ((LA16_0 == 61)) {
                    alt16 = 1;
                } else if ((LA16_0 == STRING)) {
                    alt16 = 2;
                } else {
                    if (state.backtracking > 0) {
                        state.failed = true;
                        return retval;
                    }
                    NoViableAltException nvae =
                            new NoViableAltException("", 16, 0, input);

                    throw nvae;
                }
                switch (alt16) {
                    case 1:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:93:21: '*'
                    {
                        char_literal48 = (Token) match(input, 61, FOLLOW_61_in_propertyName721);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_61.add(char_literal48);


                        // AST REWRITE
                        // elements: 61
                        // token labels: 
                        // rule labels: retval
                        // token list labels: 
                        // rule list labels: 
                        // wildcard labels: 
                        if (state.backtracking == 0) {
                            retval.tree = root_0;
                            RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "rule retval", retval != null ? retval.tree : null);

                            root_0 = (Object) adaptor.nil();
                            // 93:25: -> ^( NAME '*' )
                            {
                                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:93:28: ^( NAME '*' )
                                {
                                    Object root_1 = (Object) adaptor.nil();
                                    root_1 = (Object) adaptor.becomeRoot((Object) adaptor.create(NAME, "NAME"), root_1);

                                    adaptor.addChild(root_1, stream_61.nextNode());

                                    adaptor.addChild(root_0, root_1);
                                }

                            }

                            retval.tree = root_0;
                        }
                    }
                    break;
                    case 2:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:93:42: STRING
                    {
                        STRING49 = (Token) match(input, STRING, FOLLOW_STRING_in_propertyName733);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_STRING.add(STRING49);


                        // AST REWRITE
                        // elements: STRING
                        // token labels: 
                        // rule labels: retval
                        // token list labels: 
                        // rule list labels: 
                        // wildcard labels: 
                        if (state.backtracking == 0) {
                            retval.tree = root_0;
                            RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "rule retval", retval != null ? retval.tree : null);

                            root_0 = (Object) adaptor.nil();
                            // 93:49: -> ^( NAME STRING )
                            {
                                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:93:52: ^( NAME STRING )
                                {
                                    Object root_1 = (Object) adaptor.nil();
                                    root_1 = (Object) adaptor.becomeRoot((Object) adaptor.create(NAME, "NAME"), root_1);

                                    adaptor.addChild(root_1, stream_STRING.nextNode());

                                    adaptor.addChild(root_0, root_1);
                                }

                            }

                            retval.tree = root_0;
                        }
                    }
                    break;

                }


            }

            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "propertyName"

    public static class propertyType_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "propertyType"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:94:1: propertyType : '(' propertyTypeLiteral ')' -> ^( REQUIRED_TYPE propertyTypeLiteral ) ;
    public final CndParser.propertyType_return propertyType() throws RecognitionException {
        CndParser.propertyType_return retval = new CndParser.propertyType_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token char_literal50 = null;
        Token char_literal52 = null;
        CndParser.propertyTypeLiteral_return propertyTypeLiteral51 = null;


        Object char_literal50_tree = null;
        Object char_literal52_tree = null;
        RewriteRuleTokenStream stream_62 = new RewriteRuleTokenStream(adaptor, "token 62");
        RewriteRuleTokenStream stream_63 = new RewriteRuleTokenStream(adaptor, "token 63");
        RewriteRuleSubtreeStream stream_propertyTypeLiteral = new RewriteRuleSubtreeStream(adaptor, "rule propertyTypeLiteral");
        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:94:14: ( '(' propertyTypeLiteral ')' -> ^( REQUIRED_TYPE propertyTypeLiteral ) )
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:94:16: '(' propertyTypeLiteral ')'
            {
                char_literal50 = (Token) match(input, 62, FOLLOW_62_in_propertyType749);
                if (state.failed) return retval;
                if (state.backtracking == 0) stream_62.add(char_literal50);

                pushFollow(FOLLOW_propertyTypeLiteral_in_propertyType751);
                propertyTypeLiteral51 = propertyTypeLiteral();

                state._fsp--;
                if (state.failed) return retval;
                if (state.backtracking == 0) stream_propertyTypeLiteral.add(propertyTypeLiteral51.getTree());
                char_literal52 = (Token) match(input, 63, FOLLOW_63_in_propertyType753);
                if (state.failed) return retval;
                if (state.backtracking == 0) stream_63.add(char_literal52);


                // AST REWRITE
                // elements: propertyTypeLiteral
                // token labels: 
                // rule labels: retval
                // token list labels: 
                // rule list labels: 
                // wildcard labels: 
                if (state.backtracking == 0) {
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "rule retval", retval != null ? retval.tree : null);

                    root_0 = (Object) adaptor.nil();
                    // 94:44: -> ^( REQUIRED_TYPE propertyTypeLiteral )
                    {
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:94:47: ^( REQUIRED_TYPE propertyTypeLiteral )
                        {
                            Object root_1 = (Object) adaptor.nil();
                            root_1 = (Object) adaptor.becomeRoot((Object) adaptor.create(REQUIRED_TYPE, "REQUIRED_TYPE"), root_1);

                            adaptor.addChild(root_1, stream_propertyTypeLiteral.nextTree());

                            adaptor.addChild(root_0, root_1);
                        }

                    }

                    retval.tree = root_0;
                }
            }

            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "propertyType"

    public static class propertyTypeLiteral_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "propertyTypeLiteral"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:95:1: propertyTypeLiteral : ( 'string' | 'binary' | 'long' | 'double' | 'boolean' | 'decimal' | 'date' | 'name' | 'path' | 'reference' | '*' | 'undefined' | 'weakreference' | 'uri' ) ;
    public final CndParser.propertyTypeLiteral_return propertyTypeLiteral() throws RecognitionException {
        CndParser.propertyTypeLiteral_return retval = new CndParser.propertyTypeLiteral_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token set53 = null;

        Object set53_tree = null;

        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:95:21: ( ( 'string' | 'binary' | 'long' | 'double' | 'boolean' | 'decimal' | 'date' | 'name' | 'path' | 'reference' | '*' | 'undefined' | 'weakreference' | 'uri' ) )
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:95:23: ( 'string' | 'binary' | 'long' | 'double' | 'boolean' | 'decimal' | 'date' | 'name' | 'path' | 'reference' | '*' | 'undefined' | 'weakreference' | 'uri' )
            {
                root_0 = (Object) adaptor.nil();

                set53 = (Token) input.LT(1);
                if (input.LA(1) == 61 || (input.LA(1) >= 64 && input.LA(1) <= 76)) {
                    input.consume();
                    if (state.backtracking == 0) adaptor.addChild(root_0, (Object) adaptor.create(set53));
                    state.errorRecovery = false;
                    state.failed = false;
                } else {
                    if (state.backtracking > 0) {
                        state.failed = true;
                        return retval;
                    }
                    MismatchedSetException mse = new MismatchedSetException(null, input);
                    throw mse;
                }


            }

            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "propertyTypeLiteral"

    public static class defaultValues_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "defaultValues"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:96:1: defaultValues : '=' stringList -> ^( DEFAULT_VALUES stringList ) ;
    public final CndParser.defaultValues_return defaultValues() throws RecognitionException {
        CndParser.defaultValues_return retval = new CndParser.defaultValues_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token char_literal54 = null;
        CndParser.stringList_return stringList55 = null;


        Object char_literal54_tree = null;
        RewriteRuleTokenStream stream_43 = new RewriteRuleTokenStream(adaptor, "token 43");
        RewriteRuleSubtreeStream stream_stringList = new RewriteRuleSubtreeStream(adaptor, "rule stringList");
        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:96:15: ( '=' stringList -> ^( DEFAULT_VALUES stringList ) )
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:96:17: '=' stringList
            {
                char_literal54 = (Token) match(input, 43, FOLLOW_43_in_defaultValues803);
                if (state.failed) return retval;
                if (state.backtracking == 0) stream_43.add(char_literal54);

                pushFollow(FOLLOW_stringList_in_defaultValues805);
                stringList55 = stringList();

                state._fsp--;
                if (state.failed) return retval;
                if (state.backtracking == 0) stream_stringList.add(stringList55.getTree());


                // AST REWRITE
                // elements: stringList
                // token labels: 
                // rule labels: retval
                // token list labels: 
                // rule list labels: 
                // wildcard labels: 
                if (state.backtracking == 0) {
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "rule retval", retval != null ? retval.tree : null);

                    root_0 = (Object) adaptor.nil();
                    // 96:32: -> ^( DEFAULT_VALUES stringList )
                    {
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:96:35: ^( DEFAULT_VALUES stringList )
                        {
                            Object root_1 = (Object) adaptor.nil();
                            root_1 = (Object) adaptor.becomeRoot((Object) adaptor.create(DEFAULT_VALUES, "DEFAULT_VALUES"), root_1);

                            adaptor.addChild(root_1, stream_stringList.nextTree());

                            adaptor.addChild(root_0, root_1);
                        }

                    }

                    retval.tree = root_0;
                }
            }

            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "defaultValues"

    public static class propertyAttributes_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "propertyAttributes"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:97:1: propertyAttributes : ( ( isPrimary )=> isPrimary | ( onParentVersioningLiteral )=> onParentVersioning | ( autoCreated )=> autoCreated | ( multiple )=> multiple | ( mandatory )=> mandatory | ( isProtected )=> isProtected | ( queryOperators )=> queryOperators | ( noFullText )=> noFullText | ( noQueryOrder )=> noQueryOrder )+ ;
    public final CndParser.propertyAttributes_return propertyAttributes() throws RecognitionException {
        CndParser.propertyAttributes_return retval = new CndParser.propertyAttributes_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        CndParser.isPrimary_return isPrimary56 = null;

        CndParser.onParentVersioning_return onParentVersioning57 = null;

        CndParser.autoCreated_return autoCreated58 = null;

        CndParser.multiple_return multiple59 = null;

        CndParser.mandatory_return mandatory60 = null;

        CndParser.isProtected_return isProtected61 = null;

        CndParser.queryOperators_return queryOperators62 = null;

        CndParser.noFullText_return noFullText63 = null;

        CndParser.noQueryOrder_return noQueryOrder64 = null;


        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:97:20: ( ( ( isPrimary )=> isPrimary | ( onParentVersioningLiteral )=> onParentVersioning | ( autoCreated )=> autoCreated | ( multiple )=> multiple | ( mandatory )=> mandatory | ( isProtected )=> isProtected | ( queryOperators )=> queryOperators | ( noFullText )=> noFullText | ( noQueryOrder )=> noQueryOrder )+ )
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:97:22: ( ( isPrimary )=> isPrimary | ( onParentVersioningLiteral )=> onParentVersioning | ( autoCreated )=> autoCreated | ( multiple )=> multiple | ( mandatory )=> mandatory | ( isProtected )=> isProtected | ( queryOperators )=> queryOperators | ( noFullText )=> noFullText | ( noQueryOrder )=> noQueryOrder )+
            {
                root_0 = (Object) adaptor.nil();

                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:97:22: ( ( isPrimary )=> isPrimary | ( onParentVersioningLiteral )=> onParentVersioning | ( autoCreated )=> autoCreated | ( multiple )=> multiple | ( mandatory )=> mandatory | ( isProtected )=> isProtected | ( queryOperators )=> queryOperators | ( noFullText )=> noFullText | ( noQueryOrder )=> noQueryOrder )+
                int cnt17 = 0;
                loop17:
                do {
                    int alt17 = 10;
                    alt17 = dfa17.predict(input);
                    switch (alt17) {
                        case 1:
                            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:97:24: ( isPrimary )=> isPrimary
                        {
                            pushFollow(FOLLOW_isPrimary_in_propertyAttributes826);
                            isPrimary56 = isPrimary();

                            state._fsp--;
                            if (state.failed) return retval;
                            if (state.backtracking == 0) adaptor.addChild(root_0, isPrimary56.getTree());

                        }
                        break;
                        case 2:
                            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:97:49: ( onParentVersioningLiteral )=> onParentVersioning
                        {
                            pushFollow(FOLLOW_onParentVersioning_in_propertyAttributes834);
                            onParentVersioning57 = onParentVersioning();

                            state._fsp--;
                            if (state.failed) return retval;
                            if (state.backtracking == 0) adaptor.addChild(root_0, onParentVersioning57.getTree());

                        }
                        break;
                        case 3:
                            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:97:99: ( autoCreated )=> autoCreated
                        {
                            pushFollow(FOLLOW_autoCreated_in_propertyAttributes843);
                            autoCreated58 = autoCreated();

                            state._fsp--;
                            if (state.failed) return retval;
                            if (state.backtracking == 0) adaptor.addChild(root_0, autoCreated58.getTree());

                        }
                        break;
                        case 4:
                            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:97:129: ( multiple )=> multiple
                        {
                            pushFollow(FOLLOW_multiple_in_propertyAttributes851);
                            multiple59 = multiple();

                            state._fsp--;
                            if (state.failed) return retval;
                            if (state.backtracking == 0) adaptor.addChild(root_0, multiple59.getTree());

                        }
                        break;
                        case 5:
                            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:97:152: ( mandatory )=> mandatory
                        {
                            pushFollow(FOLLOW_mandatory_in_propertyAttributes859);
                            mandatory60 = mandatory();

                            state._fsp--;
                            if (state.failed) return retval;
                            if (state.backtracking == 0) adaptor.addChild(root_0, mandatory60.getTree());

                        }
                        break;
                        case 6:
                            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:97:177: ( isProtected )=> isProtected
                        {
                            pushFollow(FOLLOW_isProtected_in_propertyAttributes867);
                            isProtected61 = isProtected();

                            state._fsp--;
                            if (state.failed) return retval;
                            if (state.backtracking == 0) adaptor.addChild(root_0, isProtected61.getTree());

                        }
                        break;
                        case 7:
                            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:97:206: ( queryOperators )=> queryOperators
                        {
                            pushFollow(FOLLOW_queryOperators_in_propertyAttributes875);
                            queryOperators62 = queryOperators();

                            state._fsp--;
                            if (state.failed) return retval;
                            if (state.backtracking == 0) adaptor.addChild(root_0, queryOperators62.getTree());

                        }
                        break;
                        case 8:
                            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:97:241: ( noFullText )=> noFullText
                        {
                            pushFollow(FOLLOW_noFullText_in_propertyAttributes883);
                            noFullText63 = noFullText();

                            state._fsp--;
                            if (state.failed) return retval;
                            if (state.backtracking == 0) adaptor.addChild(root_0, noFullText63.getTree());

                        }
                        break;
                        case 9:
                            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:97:268: ( noQueryOrder )=> noQueryOrder
                        {
                            pushFollow(FOLLOW_noQueryOrder_in_propertyAttributes891);
                            noQueryOrder64 = noQueryOrder();

                            state._fsp--;
                            if (state.failed) return retval;
                            if (state.backtracking == 0) adaptor.addChild(root_0, noQueryOrder64.getTree());

                        }
                        break;

                        default:
                            if (cnt17 >= 1) break loop17;
                            if (state.backtracking > 0) {
                                state.failed = true;
                                return retval;
                            }
                            EarlyExitException eee =
                                    new EarlyExitException(17, input);
                            throw eee;
                    }
                    cnt17++;
                } while (true);


            }

            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "propertyAttributes"

    public static class valueConstraints_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "valueConstraints"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:99:1: valueConstraints : '<' stringList -> ^( VALUE_CONSTRAINTS stringList ) ;
    public final CndParser.valueConstraints_return valueConstraints() throws RecognitionException {
        CndParser.valueConstraints_return retval = new CndParser.valueConstraints_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token char_literal65 = null;
        CndParser.stringList_return stringList66 = null;


        Object char_literal65_tree = null;
        RewriteRuleTokenStream stream_42 = new RewriteRuleTokenStream(adaptor, "token 42");
        RewriteRuleSubtreeStream stream_stringList = new RewriteRuleSubtreeStream(adaptor, "rule stringList");
        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:99:18: ( '<' stringList -> ^( VALUE_CONSTRAINTS stringList ) )
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:99:20: '<' stringList
            {
                char_literal65 = (Token) match(input, 42, FOLLOW_42_in_valueConstraints902);
                if (state.failed) return retval;
                if (state.backtracking == 0) stream_42.add(char_literal65);

                pushFollow(FOLLOW_stringList_in_valueConstraints904);
                stringList66 = stringList();

                state._fsp--;
                if (state.failed) return retval;
                if (state.backtracking == 0) stream_stringList.add(stringList66.getTree());


                // AST REWRITE
                // elements: stringList
                // token labels: 
                // rule labels: retval
                // token list labels: 
                // rule list labels: 
                // wildcard labels: 
                if (state.backtracking == 0) {
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "rule retval", retval != null ? retval.tree : null);

                    root_0 = (Object) adaptor.nil();
                    // 99:35: -> ^( VALUE_CONSTRAINTS stringList )
                    {
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:99:38: ^( VALUE_CONSTRAINTS stringList )
                        {
                            Object root_1 = (Object) adaptor.nil();
                            root_1 = (Object) adaptor.becomeRoot((Object) adaptor.create(VALUE_CONSTRAINTS, "VALUE_CONSTRAINTS"), root_1);

                            adaptor.addChild(root_1, stream_stringList.nextTree());

                            adaptor.addChild(root_0, root_1);
                        }

                    }

                    retval.tree = root_0;
                }
            }

            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "valueConstraints"

    public static class isPrimary_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "isPrimary"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:100:1: isPrimary : ( '!' | 'pri' | 'primary' ) -> ^( IS_PRIMARY_PROPERTY STRING[\"true\"] ) ;
    public final CndParser.isPrimary_return isPrimary() throws RecognitionException {
        CndParser.isPrimary_return retval = new CndParser.isPrimary_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token char_literal67 = null;
        Token string_literal68 = null;
        Token string_literal69 = null;

        Object char_literal67_tree = null;
        Object string_literal68_tree = null;
        Object string_literal69_tree = null;
        RewriteRuleTokenStream stream_78 = new RewriteRuleTokenStream(adaptor, "token 78");
        RewriteRuleTokenStream stream_77 = new RewriteRuleTokenStream(adaptor, "token 77");
        RewriteRuleTokenStream stream_59 = new RewriteRuleTokenStream(adaptor, "token 59");

        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:100:11: ( ( '!' | 'pri' | 'primary' ) -> ^( IS_PRIMARY_PROPERTY STRING[\"true\"] ) )
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:100:13: ( '!' | 'pri' | 'primary' )
            {
                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:100:13: ( '!' | 'pri' | 'primary' )
                int alt18 = 3;
                switch (input.LA(1)) {
                    case 59: {
                        alt18 = 1;
                    }
                    break;
                    case 77: {
                        alt18 = 2;
                    }
                    break;
                    case 78: {
                        alt18 = 3;
                    }
                    break;
                    default:
                        if (state.backtracking > 0) {
                            state.failed = true;
                            return retval;
                        }
                        NoViableAltException nvae =
                                new NoViableAltException("", 18, 0, input);

                        throw nvae;
                }

                switch (alt18) {
                    case 1:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:100:14: '!'
                    {
                        char_literal67 = (Token) match(input, 59, FOLLOW_59_in_isPrimary920);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_59.add(char_literal67);


                    }
                    break;
                    case 2:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:100:18: 'pri'
                    {
                        string_literal68 = (Token) match(input, 77, FOLLOW_77_in_isPrimary922);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_77.add(string_literal68);


                    }
                    break;
                    case 3:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:100:24: 'primary'
                    {
                        string_literal69 = (Token) match(input, 78, FOLLOW_78_in_isPrimary924);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_78.add(string_literal69);


                    }
                    break;

                }


                // AST REWRITE
                // elements: 
                // token labels: 
                // rule labels: retval
                // token list labels: 
                // rule list labels: 
                // wildcard labels: 
                if (state.backtracking == 0) {
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "rule retval", retval != null ? retval.tree : null);

                    root_0 = (Object) adaptor.nil();
                    // 100:35: -> ^( IS_PRIMARY_PROPERTY STRING[\"true\"] )
                    {
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:100:38: ^( IS_PRIMARY_PROPERTY STRING[\"true\"] )
                        {
                            Object root_1 = (Object) adaptor.nil();
                            root_1 = (Object) adaptor.becomeRoot((Object) adaptor.create(IS_PRIMARY_PROPERTY, "IS_PRIMARY_PROPERTY"), root_1);

                            adaptor.addChild(root_1, (Object) adaptor.create(STRING, "true"));

                            adaptor.addChild(root_0, root_1);
                        }

                    }

                    retval.tree = root_0;
                }
            }

            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "isPrimary"

    public static class autoCreated_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "autoCreated"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:101:1: autoCreated : ( 'a' | 'aut' | 'autocreated' ) -> ^( AUTO_CREATED STRING[\"true\"] ) ;
    public final CndParser.autoCreated_return autoCreated() throws RecognitionException {
        CndParser.autoCreated_return retval = new CndParser.autoCreated_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token char_literal70 = null;
        Token string_literal71 = null;
        Token string_literal72 = null;

        Object char_literal70_tree = null;
        Object string_literal71_tree = null;
        Object string_literal72_tree = null;
        RewriteRuleTokenStream stream_79 = new RewriteRuleTokenStream(adaptor, "token 79");
        RewriteRuleTokenStream stream_53 = new RewriteRuleTokenStream(adaptor, "token 53");
        RewriteRuleTokenStream stream_80 = new RewriteRuleTokenStream(adaptor, "token 80");

        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:101:13: ( ( 'a' | 'aut' | 'autocreated' ) -> ^( AUTO_CREATED STRING[\"true\"] ) )
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:101:17: ( 'a' | 'aut' | 'autocreated' )
            {
                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:101:17: ( 'a' | 'aut' | 'autocreated' )
                int alt19 = 3;
                switch (input.LA(1)) {
                    case 53: {
                        alt19 = 1;
                    }
                    break;
                    case 79: {
                        alt19 = 2;
                    }
                    break;
                    case 80: {
                        alt19 = 3;
                    }
                    break;
                    default:
                        if (state.backtracking > 0) {
                            state.failed = true;
                            return retval;
                        }
                        NoViableAltException nvae =
                                new NoViableAltException("", 19, 0, input);

                        throw nvae;
                }

                switch (alt19) {
                    case 1:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:101:18: 'a'
                    {
                        char_literal70 = (Token) match(input, 53, FOLLOW_53_in_autoCreated944);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_53.add(char_literal70);


                    }
                    break;
                    case 2:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:101:22: 'aut'
                    {
                        string_literal71 = (Token) match(input, 79, FOLLOW_79_in_autoCreated946);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_79.add(string_literal71);


                    }
                    break;
                    case 3:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:101:28: 'autocreated'
                    {
                        string_literal72 = (Token) match(input, 80, FOLLOW_80_in_autoCreated948);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_80.add(string_literal72);


                    }
                    break;

                }


                // AST REWRITE
                // elements: 
                // token labels: 
                // rule labels: retval
                // token list labels: 
                // rule list labels: 
                // wildcard labels: 
                if (state.backtracking == 0) {
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "rule retval", retval != null ? retval.tree : null);

                    root_0 = (Object) adaptor.nil();
                    // 101:43: -> ^( AUTO_CREATED STRING[\"true\"] )
                    {
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:101:46: ^( AUTO_CREATED STRING[\"true\"] )
                        {
                            Object root_1 = (Object) adaptor.nil();
                            root_1 = (Object) adaptor.becomeRoot((Object) adaptor.create(AUTO_CREATED, "AUTO_CREATED"), root_1);

                            adaptor.addChild(root_1, (Object) adaptor.create(STRING, "true"));

                            adaptor.addChild(root_0, root_1);
                        }

                    }

                    retval.tree = root_0;
                }
            }

            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "autoCreated"

    public static class mandatory_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "mandatory"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:102:1: mandatory : ( 'm' | 'man' | 'mandatory' ) -> ^( MANDATORY STRING[\"true\"] ) ;
    public final CndParser.mandatory_return mandatory() throws RecognitionException {
        CndParser.mandatory_return retval = new CndParser.mandatory_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token char_literal73 = null;
        Token string_literal74 = null;
        Token string_literal75 = null;

        Object char_literal73_tree = null;
        Object string_literal74_tree = null;
        Object string_literal75_tree = null;
        RewriteRuleTokenStream stream_82 = new RewriteRuleTokenStream(adaptor, "token 82");
        RewriteRuleTokenStream stream_51 = new RewriteRuleTokenStream(adaptor, "token 51");
        RewriteRuleTokenStream stream_81 = new RewriteRuleTokenStream(adaptor, "token 81");

        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:102:11: ( ( 'm' | 'man' | 'mandatory' ) -> ^( MANDATORY STRING[\"true\"] ) )
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:102:13: ( 'm' | 'man' | 'mandatory' )
            {
                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:102:13: ( 'm' | 'man' | 'mandatory' )
                int alt20 = 3;
                switch (input.LA(1)) {
                    case 51: {
                        alt20 = 1;
                    }
                    break;
                    case 81: {
                        alt20 = 2;
                    }
                    break;
                    case 82: {
                        alt20 = 3;
                    }
                    break;
                    default:
                        if (state.backtracking > 0) {
                            state.failed = true;
                            return retval;
                        }
                        NoViableAltException nvae =
                                new NoViableAltException("", 20, 0, input);

                        throw nvae;
                }

                switch (alt20) {
                    case 1:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:102:14: 'm'
                    {
                        char_literal73 = (Token) match(input, 51, FOLLOW_51_in_mandatory966);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_51.add(char_literal73);


                    }
                    break;
                    case 2:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:102:18: 'man'
                    {
                        string_literal74 = (Token) match(input, 81, FOLLOW_81_in_mandatory968);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_81.add(string_literal74);


                    }
                    break;
                    case 3:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:102:24: 'mandatory'
                    {
                        string_literal75 = (Token) match(input, 82, FOLLOW_82_in_mandatory970);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_82.add(string_literal75);


                    }
                    break;

                }


                // AST REWRITE
                // elements: 
                // token labels: 
                // rule labels: retval
                // token list labels: 
                // rule list labels: 
                // wildcard labels: 
                if (state.backtracking == 0) {
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "rule retval", retval != null ? retval.tree : null);

                    root_0 = (Object) adaptor.nil();
                    // 102:37: -> ^( MANDATORY STRING[\"true\"] )
                    {
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:102:40: ^( MANDATORY STRING[\"true\"] )
                        {
                            Object root_1 = (Object) adaptor.nil();
                            root_1 = (Object) adaptor.becomeRoot((Object) adaptor.create(MANDATORY, "MANDATORY"), root_1);

                            adaptor.addChild(root_1, (Object) adaptor.create(STRING, "true"));

                            adaptor.addChild(root_0, root_1);
                        }

                    }

                    retval.tree = root_0;
                }
            }

            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "mandatory"

    public static class isProtected_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "isProtected"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:103:1: isProtected : ( 'p' | 'pro' | 'protected' ) -> ^( PROTECTED STRING[\"true\"] ) ;
    public final CndParser.isProtected_return isProtected() throws RecognitionException {
        CndParser.isProtected_return retval = new CndParser.isProtected_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token char_literal76 = null;
        Token string_literal77 = null;
        Token string_literal78 = null;

        Object char_literal76_tree = null;
        Object string_literal77_tree = null;
        Object string_literal78_tree = null;
        RewriteRuleTokenStream stream_83 = new RewriteRuleTokenStream(adaptor, "token 83");
        RewriteRuleTokenStream stream_84 = new RewriteRuleTokenStream(adaptor, "token 84");
        RewriteRuleTokenStream stream_85 = new RewriteRuleTokenStream(adaptor, "token 85");

        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:103:13: ( ( 'p' | 'pro' | 'protected' ) -> ^( PROTECTED STRING[\"true\"] ) )
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:103:17: ( 'p' | 'pro' | 'protected' )
            {
                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:103:17: ( 'p' | 'pro' | 'protected' )
                int alt21 = 3;
                switch (input.LA(1)) {
                    case 83: {
                        alt21 = 1;
                    }
                    break;
                    case 84: {
                        alt21 = 2;
                    }
                    break;
                    case 85: {
                        alt21 = 3;
                    }
                    break;
                    default:
                        if (state.backtracking > 0) {
                            state.failed = true;
                            return retval;
                        }
                        NoViableAltException nvae =
                                new NoViableAltException("", 21, 0, input);

                        throw nvae;
                }

                switch (alt21) {
                    case 1:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:103:18: 'p'
                    {
                        char_literal76 = (Token) match(input, 83, FOLLOW_83_in_isProtected990);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_83.add(char_literal76);


                    }
                    break;
                    case 2:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:103:22: 'pro'
                    {
                        string_literal77 = (Token) match(input, 84, FOLLOW_84_in_isProtected992);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_84.add(string_literal77);


                    }
                    break;
                    case 3:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:103:28: 'protected'
                    {
                        string_literal78 = (Token) match(input, 85, FOLLOW_85_in_isProtected994);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_85.add(string_literal78);


                    }
                    break;

                }


                // AST REWRITE
                // elements: 
                // token labels: 
                // rule labels: retval
                // token list labels: 
                // rule list labels: 
                // wildcard labels: 
                if (state.backtracking == 0) {
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "rule retval", retval != null ? retval.tree : null);

                    root_0 = (Object) adaptor.nil();
                    // 103:41: -> ^( PROTECTED STRING[\"true\"] )
                    {
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:103:44: ^( PROTECTED STRING[\"true\"] )
                        {
                            Object root_1 = (Object) adaptor.nil();
                            root_1 = (Object) adaptor.becomeRoot((Object) adaptor.create(PROTECTED, "PROTECTED"), root_1);

                            adaptor.addChild(root_1, (Object) adaptor.create(STRING, "true"));

                            adaptor.addChild(root_0, root_1);
                        }

                    }

                    retval.tree = root_0;
                }
            }

            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "isProtected"

    public static class onParentVersioning_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "onParentVersioning"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:104:1: onParentVersioning : onParentVersioningLiteral -> ^( ON_PARENT_VERSION onParentVersioningLiteral ) ;
    public final CndParser.onParentVersioning_return onParentVersioning() throws RecognitionException {
        CndParser.onParentVersioning_return retval = new CndParser.onParentVersioning_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        CndParser.onParentVersioningLiteral_return onParentVersioningLiteral79 = null;


        RewriteRuleSubtreeStream stream_onParentVersioningLiteral = new RewriteRuleSubtreeStream(adaptor, "rule onParentVersioningLiteral");
        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:104:20: ( onParentVersioningLiteral -> ^( ON_PARENT_VERSION onParentVersioningLiteral ) )
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:104:25: onParentVersioningLiteral
            {
                pushFollow(FOLLOW_onParentVersioningLiteral_in_onParentVersioning1014);
                onParentVersioningLiteral79 = onParentVersioningLiteral();

                state._fsp--;
                if (state.failed) return retval;
                if (state.backtracking == 0)
                    stream_onParentVersioningLiteral.add(onParentVersioningLiteral79.getTree());


                // AST REWRITE
                // elements: onParentVersioningLiteral
                // token labels: 
                // rule labels: retval
                // token list labels: 
                // rule list labels: 
                // wildcard labels: 
                if (state.backtracking == 0) {
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "rule retval", retval != null ? retval.tree : null);

                    root_0 = (Object) adaptor.nil();
                    // 104:51: -> ^( ON_PARENT_VERSION onParentVersioningLiteral )
                    {
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:104:54: ^( ON_PARENT_VERSION onParentVersioningLiteral )
                        {
                            Object root_1 = (Object) adaptor.nil();
                            root_1 = (Object) adaptor.becomeRoot((Object) adaptor.create(ON_PARENT_VERSION, "ON_PARENT_VERSION"), root_1);

                            adaptor.addChild(root_1, stream_onParentVersioningLiteral.nextTree());

                            adaptor.addChild(root_0, root_1);
                        }

                    }

                    retval.tree = root_0;
                }
            }

            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "onParentVersioning"

    public static class onParentVersioningLiteral_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "onParentVersioningLiteral"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:105:1: onParentVersioningLiteral : ( 'copy' | 'version' | 'initialize' | 'compute' | 'ignore' | 'abort' ) ;
    public final CndParser.onParentVersioningLiteral_return onParentVersioningLiteral() throws RecognitionException {
        CndParser.onParentVersioningLiteral_return retval = new CndParser.onParentVersioningLiteral_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token set80 = null;

        Object set80_tree = null;

        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:105:27: ( ( 'copy' | 'version' | 'initialize' | 'compute' | 'ignore' | 'abort' ) )
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:105:29: ( 'copy' | 'version' | 'initialize' | 'compute' | 'ignore' | 'abort' )
            {
                root_0 = (Object) adaptor.nil();

                set80 = (Token) input.LT(1);
                if ((input.LA(1) >= 86 && input.LA(1) <= 91)) {
                    input.consume();
                    if (state.backtracking == 0) adaptor.addChild(root_0, (Object) adaptor.create(set80));
                    state.errorRecovery = false;
                    state.failed = false;
                } else {
                    if (state.backtracking > 0) {
                        state.failed = true;
                        return retval;
                    }
                    MismatchedSetException mse = new MismatchedSetException(null, input);
                    throw mse;
                }


            }

            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "onParentVersioningLiteral"

    public static class multiple_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "multiple"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:106:1: multiple : ( '*' | 'mul' | 'multiple' ) -> ^( MULTIPLE STRING[\"true\"] ) ;
    public final CndParser.multiple_return multiple() throws RecognitionException {
        CndParser.multiple_return retval = new CndParser.multiple_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token char_literal81 = null;
        Token string_literal82 = null;
        Token string_literal83 = null;

        Object char_literal81_tree = null;
        Object string_literal82_tree = null;
        Object string_literal83_tree = null;
        RewriteRuleTokenStream stream_93 = new RewriteRuleTokenStream(adaptor, "token 93");
        RewriteRuleTokenStream stream_92 = new RewriteRuleTokenStream(adaptor, "token 92");
        RewriteRuleTokenStream stream_61 = new RewriteRuleTokenStream(adaptor, "token 61");

        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:106:10: ( ( '*' | 'mul' | 'multiple' ) -> ^( MULTIPLE STRING[\"true\"] ) )
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:106:13: ( '*' | 'mul' | 'multiple' )
            {
                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:106:13: ( '*' | 'mul' | 'multiple' )
                int alt22 = 3;
                switch (input.LA(1)) {
                    case 61: {
                        alt22 = 1;
                    }
                    break;
                    case 92: {
                        alt22 = 2;
                    }
                    break;
                    case 93: {
                        alt22 = 3;
                    }
                    break;
                    default:
                        if (state.backtracking > 0) {
                            state.failed = true;
                            return retval;
                        }
                        NoViableAltException nvae =
                                new NoViableAltException("", 22, 0, input);

                        throw nvae;
                }

                switch (alt22) {
                    case 1:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:106:14: '*'
                    {
                        char_literal81 = (Token) match(input, 61, FOLLOW_61_in_multiple1050);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_61.add(char_literal81);


                    }
                    break;
                    case 2:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:106:18: 'mul'
                    {
                        string_literal82 = (Token) match(input, 92, FOLLOW_92_in_multiple1052);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_92.add(string_literal82);


                    }
                    break;
                    case 3:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:106:24: 'multiple'
                    {
                        string_literal83 = (Token) match(input, 93, FOLLOW_93_in_multiple1054);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_93.add(string_literal83);


                    }
                    break;

                }


                // AST REWRITE
                // elements: 
                // token labels: 
                // rule labels: retval
                // token list labels: 
                // rule list labels: 
                // wildcard labels: 
                if (state.backtracking == 0) {
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "rule retval", retval != null ? retval.tree : null);

                    root_0 = (Object) adaptor.nil();
                    // 106:36: -> ^( MULTIPLE STRING[\"true\"] )
                    {
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:106:39: ^( MULTIPLE STRING[\"true\"] )
                        {
                            Object root_1 = (Object) adaptor.nil();
                            root_1 = (Object) adaptor.becomeRoot((Object) adaptor.create(MULTIPLE, "MULTIPLE"), root_1);

                            adaptor.addChild(root_1, (Object) adaptor.create(STRING, "true"));

                            adaptor.addChild(root_0, root_1);
                        }

                    }

                    retval.tree = root_0;
                }
            }

            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "multiple"

    public static class noFullText_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "noFullText"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:107:1: noFullText : ( 'nof' | 'nofulltext' ) -> ^( IS_FULL_TEXT_SEARCHABLE STRING[\"false\"] ) ;
    public final CndParser.noFullText_return noFullText() throws RecognitionException {
        CndParser.noFullText_return retval = new CndParser.noFullText_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token string_literal84 = null;
        Token string_literal85 = null;

        Object string_literal84_tree = null;
        Object string_literal85_tree = null;
        RewriteRuleTokenStream stream_95 = new RewriteRuleTokenStream(adaptor, "token 95");
        RewriteRuleTokenStream stream_94 = new RewriteRuleTokenStream(adaptor, "token 94");

        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:107:12: ( ( 'nof' | 'nofulltext' ) -> ^( IS_FULL_TEXT_SEARCHABLE STRING[\"false\"] ) )
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:107:17: ( 'nof' | 'nofulltext' )
            {
                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:107:17: ( 'nof' | 'nofulltext' )
                int alt23 = 2;
                int LA23_0 = input.LA(1);

                if ((LA23_0 == 94)) {
                    alt23 = 1;
                } else if ((LA23_0 == 95)) {
                    alt23 = 2;
                } else {
                    if (state.backtracking > 0) {
                        state.failed = true;
                        return retval;
                    }
                    NoViableAltException nvae =
                            new NoViableAltException("", 23, 0, input);

                    throw nvae;
                }
                switch (alt23) {
                    case 1:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:107:18: 'nof'
                    {
                        string_literal84 = (Token) match(input, 94, FOLLOW_94_in_noFullText1075);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_94.add(string_literal84);


                    }
                    break;
                    case 2:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:107:24: 'nofulltext'
                    {
                        string_literal85 = (Token) match(input, 95, FOLLOW_95_in_noFullText1077);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_95.add(string_literal85);


                    }
                    break;

                }


                // AST REWRITE
                // elements: 
                // token labels: 
                // rule labels: retval
                // token list labels: 
                // rule list labels: 
                // wildcard labels: 
                if (state.backtracking == 0) {
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "rule retval", retval != null ? retval.tree : null);

                    root_0 = (Object) adaptor.nil();
                    // 107:38: -> ^( IS_FULL_TEXT_SEARCHABLE STRING[\"false\"] )
                    {
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:107:41: ^( IS_FULL_TEXT_SEARCHABLE STRING[\"false\"] )
                        {
                            Object root_1 = (Object) adaptor.nil();
                            root_1 = (Object) adaptor.becomeRoot((Object) adaptor.create(IS_FULL_TEXT_SEARCHABLE, "IS_FULL_TEXT_SEARCHABLE"), root_1);

                            adaptor.addChild(root_1, (Object) adaptor.create(STRING, "false"));

                            adaptor.addChild(root_0, root_1);
                        }

                    }

                    retval.tree = root_0;
                }
            }

            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "noFullText"

    public static class noQueryOrder_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "noQueryOrder"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:108:1: noQueryOrder : ( 'nqord' | 'noqueryorder' ) -> ^( IS_QUERY_ORDERERABLE STRING[\"false\"] ) ;
    public final CndParser.noQueryOrder_return noQueryOrder() throws RecognitionException {
        CndParser.noQueryOrder_return retval = new CndParser.noQueryOrder_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token string_literal86 = null;
        Token string_literal87 = null;

        Object string_literal86_tree = null;
        Object string_literal87_tree = null;
        RewriteRuleTokenStream stream_97 = new RewriteRuleTokenStream(adaptor, "token 97");
        RewriteRuleTokenStream stream_96 = new RewriteRuleTokenStream(adaptor, "token 96");

        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:108:14: ( ( 'nqord' | 'noqueryorder' ) -> ^( IS_QUERY_ORDERERABLE STRING[\"false\"] ) )
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:108:17: ( 'nqord' | 'noqueryorder' )
            {
                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:108:17: ( 'nqord' | 'noqueryorder' )
                int alt24 = 2;
                int LA24_0 = input.LA(1);

                if ((LA24_0 == 96)) {
                    alt24 = 1;
                } else if ((LA24_0 == 97)) {
                    alt24 = 2;
                } else {
                    if (state.backtracking > 0) {
                        state.failed = true;
                        return retval;
                    }
                    NoViableAltException nvae =
                            new NoViableAltException("", 24, 0, input);

                    throw nvae;
                }
                switch (alt24) {
                    case 1:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:108:18: 'nqord'
                    {
                        string_literal86 = (Token) match(input, 96, FOLLOW_96_in_noQueryOrder1096);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_96.add(string_literal86);


                    }
                    break;
                    case 2:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:108:26: 'noqueryorder'
                    {
                        string_literal87 = (Token) match(input, 97, FOLLOW_97_in_noQueryOrder1098);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_97.add(string_literal87);


                    }
                    break;

                }


                // AST REWRITE
                // elements: 
                // token labels: 
                // rule labels: retval
                // token list labels: 
                // rule list labels: 
                // wildcard labels: 
                if (state.backtracking == 0) {
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "rule retval", retval != null ? retval.tree : null);

                    root_0 = (Object) adaptor.nil();
                    // 108:42: -> ^( IS_QUERY_ORDERERABLE STRING[\"false\"] )
                    {
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:108:45: ^( IS_QUERY_ORDERERABLE STRING[\"false\"] )
                        {
                            Object root_1 = (Object) adaptor.nil();
                            root_1 = (Object) adaptor.becomeRoot((Object) adaptor.create(IS_QUERY_ORDERERABLE, "IS_QUERY_ORDERERABLE"), root_1);

                            adaptor.addChild(root_1, (Object) adaptor.create(STRING, "false"));

                            adaptor.addChild(root_0, root_1);
                        }

                    }

                    retval.tree = root_0;
                }
            }

            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "noQueryOrder"

    public static class queryOperators_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "queryOperators"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:109:1: queryOperators : ( 'qop' | 'queryops' ) STRING -> ^( QUERY_OPERATORS STRING ) ;
    public final CndParser.queryOperators_return queryOperators() throws RecognitionException {
        CndParser.queryOperators_return retval = new CndParser.queryOperators_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token string_literal88 = null;
        Token string_literal89 = null;
        Token STRING90 = null;

        Object string_literal88_tree = null;
        Object string_literal89_tree = null;
        Object STRING90_tree = null;
        RewriteRuleTokenStream stream_98 = new RewriteRuleTokenStream(adaptor, "token 98");
        RewriteRuleTokenStream stream_99 = new RewriteRuleTokenStream(adaptor, "token 99");
        RewriteRuleTokenStream stream_STRING = new RewriteRuleTokenStream(adaptor, "token STRING");

        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:109:16: ( ( 'qop' | 'queryops' ) STRING -> ^( QUERY_OPERATORS STRING ) )
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:109:21: ( 'qop' | 'queryops' ) STRING
            {
                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:109:21: ( 'qop' | 'queryops' )
                int alt25 = 2;
                int LA25_0 = input.LA(1);

                if ((LA25_0 == 98)) {
                    alt25 = 1;
                } else if ((LA25_0 == 99)) {
                    alt25 = 2;
                } else {
                    if (state.backtracking > 0) {
                        state.failed = true;
                        return retval;
                    }
                    NoViableAltException nvae =
                            new NoViableAltException("", 25, 0, input);

                    throw nvae;
                }
                switch (alt25) {
                    case 1:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:109:22: 'qop'
                    {
                        string_literal88 = (Token) match(input, 98, FOLLOW_98_in_queryOperators1119);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_98.add(string_literal88);


                    }
                    break;
                    case 2:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:109:28: 'queryops'
                    {
                        string_literal89 = (Token) match(input, 99, FOLLOW_99_in_queryOperators1121);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_99.add(string_literal89);


                    }
                    break;

                }

                STRING90 = (Token) match(input, STRING, FOLLOW_STRING_in_queryOperators1124);
                if (state.failed) return retval;
                if (state.backtracking == 0) stream_STRING.add(STRING90);


                // AST REWRITE
                // elements: STRING
                // token labels: 
                // rule labels: retval
                // token list labels: 
                // rule list labels: 
                // wildcard labels: 
                if (state.backtracking == 0) {
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "rule retval", retval != null ? retval.tree : null);

                    root_0 = (Object) adaptor.nil();
                    // 109:47: -> ^( QUERY_OPERATORS STRING )
                    {
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:109:50: ^( QUERY_OPERATORS STRING )
                        {
                            Object root_1 = (Object) adaptor.nil();
                            root_1 = (Object) adaptor.becomeRoot((Object) adaptor.create(QUERY_OPERATORS, "QUERY_OPERATORS"), root_1);

                            adaptor.addChild(root_1, stream_STRING.nextNode());

                            adaptor.addChild(root_0, root_1);
                        }

                    }

                    retval.tree = root_0;
                }
            }

            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "queryOperators"

    public static class childNodeDefinition_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "childNodeDefinition"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:115:1: childNodeDefinition : nodeName ( requiredTypes )? ( defaultType )? ( nodeAttributes )? -> ^( NODE nodeName ^( PRIMARY_TYPE STRING[\"nt:childNodeDefinition\"] ) ( requiredTypes )? ( defaultType )? ( nodeAttributes )? ) ;
    public final CndParser.childNodeDefinition_return childNodeDefinition() throws RecognitionException {
        CndParser.childNodeDefinition_return retval = new CndParser.childNodeDefinition_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        CndParser.nodeName_return nodeName91 = null;

        CndParser.requiredTypes_return requiredTypes92 = null;

        CndParser.defaultType_return defaultType93 = null;

        CndParser.nodeAttributes_return nodeAttributes94 = null;


        RewriteRuleSubtreeStream stream_nodeName = new RewriteRuleSubtreeStream(adaptor, "rule nodeName");
        RewriteRuleSubtreeStream stream_defaultType = new RewriteRuleSubtreeStream(adaptor, "rule defaultType");
        RewriteRuleSubtreeStream stream_requiredTypes = new RewriteRuleSubtreeStream(adaptor, "rule requiredTypes");
        RewriteRuleSubtreeStream stream_nodeAttributes = new RewriteRuleSubtreeStream(adaptor, "rule nodeAttributes");
        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:115:21: ( nodeName ( requiredTypes )? ( defaultType )? ( nodeAttributes )? -> ^( NODE nodeName ^( PRIMARY_TYPE STRING[\"nt:childNodeDefinition\"] ) ( requiredTypes )? ( defaultType )? ( nodeAttributes )? ) )
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:115:23: nodeName ( requiredTypes )? ( defaultType )? ( nodeAttributes )?
            {
                pushFollow(FOLLOW_nodeName_in_childNodeDefinition1144);
                nodeName91 = nodeName();

                state._fsp--;
                if (state.failed) return retval;
                if (state.backtracking == 0) stream_nodeName.add(nodeName91.getTree());
                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:115:32: ( requiredTypes )?
                int alt26 = 2;
                int LA26_0 = input.LA(1);

                if ((LA26_0 == 62)) {
                    alt26 = 1;
                }
                switch (alt26) {
                    case 1:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:115:32: requiredTypes
                    {
                        pushFollow(FOLLOW_requiredTypes_in_childNodeDefinition1146);
                        requiredTypes92 = requiredTypes();

                        state._fsp--;
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_requiredTypes.add(requiredTypes92.getTree());

                    }
                    break;

                }

                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:115:47: ( defaultType )?
                int alt27 = 2;
                int LA27_0 = input.LA(1);

                if ((LA27_0 == 43)) {
                    alt27 = 1;
                }
                switch (alt27) {
                    case 1:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:115:47: defaultType
                    {
                        pushFollow(FOLLOW_defaultType_in_childNodeDefinition1149);
                        defaultType93 = defaultType();

                        state._fsp--;
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_defaultType.add(defaultType93.getTree());

                    }
                    break;

                }

                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:115:60: ( nodeAttributes )?
                int alt28 = 2;
                int LA28_0 = input.LA(1);

                if ((LA28_0 == 51 || LA28_0 == 53 || LA28_0 == 59 || LA28_0 == 61 || (LA28_0 >= 77 && LA28_0 <= 91) || LA28_0 == 93 || LA28_0 == 101)) {
                    alt28 = 1;
                }
                switch (alt28) {
                    case 1:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:115:60: nodeAttributes
                    {
                        pushFollow(FOLLOW_nodeAttributes_in_childNodeDefinition1152);
                        nodeAttributes94 = nodeAttributes();

                        state._fsp--;
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_nodeAttributes.add(nodeAttributes94.getTree());

                    }
                    break;

                }


                // AST REWRITE
                // elements: defaultType, nodeAttributes, nodeName, requiredTypes
                // token labels: 
                // rule labels: retval
                // token list labels: 
                // rule list labels: 
                // wildcard labels: 
                if (state.backtracking == 0) {
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "rule retval", retval != null ? retval.tree : null);

                    root_0 = (Object) adaptor.nil();
                    // 116:3: -> ^( NODE nodeName ^( PRIMARY_TYPE STRING[\"nt:childNodeDefinition\"] ) ( requiredTypes )? ( defaultType )? ( nodeAttributes )? )
                    {
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:116:6: ^( NODE nodeName ^( PRIMARY_TYPE STRING[\"nt:childNodeDefinition\"] ) ( requiredTypes )? ( defaultType )? ( nodeAttributes )? )
                        {
                            Object root_1 = (Object) adaptor.nil();
                            root_1 = (Object) adaptor.becomeRoot((Object) adaptor.create(NODE, "NODE"), root_1);

                            adaptor.addChild(root_1, stream_nodeName.nextTree());
                            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:116:22: ^( PRIMARY_TYPE STRING[\"nt:childNodeDefinition\"] )
                            {
                                Object root_2 = (Object) adaptor.nil();
                                root_2 = (Object) adaptor.becomeRoot((Object) adaptor.create(PRIMARY_TYPE, "PRIMARY_TYPE"), root_2);

                                adaptor.addChild(root_2, (Object) adaptor.create(STRING, "nt:childNodeDefinition"));

                                adaptor.addChild(root_1, root_2);
                            }
                            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:116:71: ( requiredTypes )?
                            if (stream_requiredTypes.hasNext()) {
                                adaptor.addChild(root_1, stream_requiredTypes.nextTree());

                            }
                            stream_requiredTypes.reset();
                            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:116:86: ( defaultType )?
                            if (stream_defaultType.hasNext()) {
                                adaptor.addChild(root_1, stream_defaultType.nextTree());

                            }
                            stream_defaultType.reset();
                            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:116:99: ( nodeAttributes )?
                            if (stream_nodeAttributes.hasNext()) {
                                adaptor.addChild(root_1, stream_nodeAttributes.nextTree());

                            }
                            stream_nodeAttributes.reset();

                            adaptor.addChild(root_0, root_1);
                        }

                    }

                    retval.tree = root_0;
                }
            }

            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "childNodeDefinition"

    public static class nodeName_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "nodeName"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:117:1: nodeName : '+' ( '*' -> ^( NAME '*' ) | STRING -> ^( NAME STRING ) ) ;
    public final CndParser.nodeName_return nodeName() throws RecognitionException {
        CndParser.nodeName_return retval = new CndParser.nodeName_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token char_literal95 = null;
        Token char_literal96 = null;
        Token STRING97 = null;

        Object char_literal95_tree = null;
        Object char_literal96_tree = null;
        Object STRING97_tree = null;
        RewriteRuleTokenStream stream_61 = new RewriteRuleTokenStream(adaptor, "token 61");
        RewriteRuleTokenStream stream_STRING = new RewriteRuleTokenStream(adaptor, "token STRING");
        RewriteRuleTokenStream stream_100 = new RewriteRuleTokenStream(adaptor, "token 100");

        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:117:10: ( '+' ( '*' -> ^( NAME '*' ) | STRING -> ^( NAME STRING ) ) )
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:117:12: '+' ( '*' -> ^( NAME '*' ) | STRING -> ^( NAME STRING ) )
            {
                char_literal95 = (Token) match(input, 100, FOLLOW_100_in_nodeName1186);
                if (state.failed) return retval;
                if (state.backtracking == 0) stream_100.add(char_literal95);

                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:117:16: ( '*' -> ^( NAME '*' ) | STRING -> ^( NAME STRING ) )
                int alt29 = 2;
                int LA29_0 = input.LA(1);

                if ((LA29_0 == 61)) {
                    alt29 = 1;
                } else if ((LA29_0 == STRING)) {
                    alt29 = 2;
                } else {
                    if (state.backtracking > 0) {
                        state.failed = true;
                        return retval;
                    }
                    NoViableAltException nvae =
                            new NoViableAltException("", 29, 0, input);

                    throw nvae;
                }
                switch (alt29) {
                    case 1:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:117:17: '*'
                    {
                        char_literal96 = (Token) match(input, 61, FOLLOW_61_in_nodeName1189);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_61.add(char_literal96);


                        // AST REWRITE
                        // elements: 61
                        // token labels: 
                        // rule labels: retval
                        // token list labels: 
                        // rule list labels: 
                        // wildcard labels: 
                        if (state.backtracking == 0) {
                            retval.tree = root_0;
                            RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "rule retval", retval != null ? retval.tree : null);

                            root_0 = (Object) adaptor.nil();
                            // 117:21: -> ^( NAME '*' )
                            {
                                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:117:24: ^( NAME '*' )
                                {
                                    Object root_1 = (Object) adaptor.nil();
                                    root_1 = (Object) adaptor.becomeRoot((Object) adaptor.create(NAME, "NAME"), root_1);

                                    adaptor.addChild(root_1, stream_61.nextNode());

                                    adaptor.addChild(root_0, root_1);
                                }

                            }

                            retval.tree = root_0;
                        }
                    }
                    break;
                    case 2:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:117:38: STRING
                    {
                        STRING97 = (Token) match(input, STRING, FOLLOW_STRING_in_nodeName1201);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_STRING.add(STRING97);


                        // AST REWRITE
                        // elements: STRING
                        // token labels: 
                        // rule labels: retval
                        // token list labels: 
                        // rule list labels: 
                        // wildcard labels: 
                        if (state.backtracking == 0) {
                            retval.tree = root_0;
                            RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "rule retval", retval != null ? retval.tree : null);

                            root_0 = (Object) adaptor.nil();
                            // 117:45: -> ^( NAME STRING )
                            {
                                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:117:48: ^( NAME STRING )
                                {
                                    Object root_1 = (Object) adaptor.nil();
                                    root_1 = (Object) adaptor.becomeRoot((Object) adaptor.create(NAME, "NAME"), root_1);

                                    adaptor.addChild(root_1, stream_STRING.nextNode());

                                    adaptor.addChild(root_0, root_1);
                                }

                            }

                            retval.tree = root_0;
                        }
                    }
                    break;

                }


            }

            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "nodeName"

    public static class requiredTypes_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "requiredTypes"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:118:1: requiredTypes : '(' stringList ')' -> ^( REQUIRED_PRIMARY_TYPES stringList ) ;
    public final CndParser.requiredTypes_return requiredTypes() throws RecognitionException {
        CndParser.requiredTypes_return retval = new CndParser.requiredTypes_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token char_literal98 = null;
        Token char_literal100 = null;
        CndParser.stringList_return stringList99 = null;


        Object char_literal98_tree = null;
        Object char_literal100_tree = null;
        RewriteRuleTokenStream stream_62 = new RewriteRuleTokenStream(adaptor, "token 62");
        RewriteRuleTokenStream stream_63 = new RewriteRuleTokenStream(adaptor, "token 63");
        RewriteRuleSubtreeStream stream_stringList = new RewriteRuleSubtreeStream(adaptor, "rule stringList");
        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:118:15: ( '(' stringList ')' -> ^( REQUIRED_PRIMARY_TYPES stringList ) )
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:118:16: '(' stringList ')'
            {
                char_literal98 = (Token) match(input, 62, FOLLOW_62_in_requiredTypes1216);
                if (state.failed) return retval;
                if (state.backtracking == 0) stream_62.add(char_literal98);

                pushFollow(FOLLOW_stringList_in_requiredTypes1218);
                stringList99 = stringList();

                state._fsp--;
                if (state.failed) return retval;
                if (state.backtracking == 0) stream_stringList.add(stringList99.getTree());
                char_literal100 = (Token) match(input, 63, FOLLOW_63_in_requiredTypes1220);
                if (state.failed) return retval;
                if (state.backtracking == 0) stream_63.add(char_literal100);


                // AST REWRITE
                // elements: stringList
                // token labels: 
                // rule labels: retval
                // token list labels: 
                // rule list labels: 
                // wildcard labels: 
                if (state.backtracking == 0) {
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "rule retval", retval != null ? retval.tree : null);

                    root_0 = (Object) adaptor.nil();
                    // 118:35: -> ^( REQUIRED_PRIMARY_TYPES stringList )
                    {
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:118:38: ^( REQUIRED_PRIMARY_TYPES stringList )
                        {
                            Object root_1 = (Object) adaptor.nil();
                            root_1 = (Object) adaptor.becomeRoot((Object) adaptor.create(REQUIRED_PRIMARY_TYPES, "REQUIRED_PRIMARY_TYPES"), root_1);

                            adaptor.addChild(root_1, stream_stringList.nextTree());

                            adaptor.addChild(root_0, root_1);
                        }

                    }

                    retval.tree = root_0;
                }
            }

            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "requiredTypes"

    public static class defaultType_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "defaultType"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:119:1: defaultType : '=' STRING -> ^( DEFAULT_PRIMARY_TYPE STRING ) ;
    public final CndParser.defaultType_return defaultType() throws RecognitionException {
        CndParser.defaultType_return retval = new CndParser.defaultType_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token char_literal101 = null;
        Token STRING102 = null;

        Object char_literal101_tree = null;
        Object STRING102_tree = null;
        RewriteRuleTokenStream stream_43 = new RewriteRuleTokenStream(adaptor, "token 43");
        RewriteRuleTokenStream stream_STRING = new RewriteRuleTokenStream(adaptor, "token STRING");

        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:119:13: ( '=' STRING -> ^( DEFAULT_PRIMARY_TYPE STRING ) )
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:119:17: '=' STRING
            {
                char_literal101 = (Token) match(input, 43, FOLLOW_43_in_defaultType1237);
                if (state.failed) return retval;
                if (state.backtracking == 0) stream_43.add(char_literal101);

                STRING102 = (Token) match(input, STRING, FOLLOW_STRING_in_defaultType1239);
                if (state.failed) return retval;
                if (state.backtracking == 0) stream_STRING.add(STRING102);


                // AST REWRITE
                // elements: STRING
                // token labels: 
                // rule labels: retval
                // token list labels: 
                // rule list labels: 
                // wildcard labels: 
                if (state.backtracking == 0) {
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "rule retval", retval != null ? retval.tree : null);

                    root_0 = (Object) adaptor.nil();
                    // 119:28: -> ^( DEFAULT_PRIMARY_TYPE STRING )
                    {
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:119:31: ^( DEFAULT_PRIMARY_TYPE STRING )
                        {
                            Object root_1 = (Object) adaptor.nil();
                            root_1 = (Object) adaptor.becomeRoot((Object) adaptor.create(DEFAULT_PRIMARY_TYPE, "DEFAULT_PRIMARY_TYPE"), root_1);

                            adaptor.addChild(root_1, stream_STRING.nextNode());

                            adaptor.addChild(root_0, root_1);
                        }

                    }

                    retval.tree = root_0;
                }
            }

            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "defaultType"

    public static class nodeAttributes_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "nodeAttributes"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:120:1: nodeAttributes : ( nodeAttribute )+ ;
    public final CndParser.nodeAttributes_return nodeAttributes() throws RecognitionException {
        CndParser.nodeAttributes_return retval = new CndParser.nodeAttributes_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        CndParser.nodeAttribute_return nodeAttribute103 = null;


        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:120:16: ( ( nodeAttribute )+ )
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:120:18: ( nodeAttribute )+
            {
                root_0 = (Object) adaptor.nil();

                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:120:18: ( nodeAttribute )+
                int cnt30 = 0;
                loop30:
                do {
                    int alt30 = 2;
                    int LA30_0 = input.LA(1);

                    if ((LA30_0 == 51 || LA30_0 == 53 || LA30_0 == 59 || LA30_0 == 61 || (LA30_0 >= 77 && LA30_0 <= 91) || LA30_0 == 93 || LA30_0 == 101)) {
                        alt30 = 1;
                    }


                    switch (alt30) {
                        case 1:
                            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:120:18: nodeAttribute
                        {
                            pushFollow(FOLLOW_nodeAttribute_in_nodeAttributes1254);
                            nodeAttribute103 = nodeAttribute();

                            state._fsp--;
                            if (state.failed) return retval;
                            if (state.backtracking == 0) adaptor.addChild(root_0, nodeAttribute103.getTree());

                        }
                        break;

                        default:
                            if (cnt30 >= 1) break loop30;
                            if (state.backtracking > 0) {
                                state.failed = true;
                                return retval;
                            }
                            EarlyExitException eee =
                                    new EarlyExitException(30, input);
                            throw eee;
                    }
                    cnt30++;
                } while (true);


            }

            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "nodeAttributes"

    public static class nodeAttribute_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "nodeAttribute"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:121:1: nodeAttribute : ( autoCreated | mandatory | ( isPrimary )=> isPrimary | ( isProtected )=> isProtected | onParentVersioning | sns );
    public final CndParser.nodeAttribute_return nodeAttribute() throws RecognitionException {
        CndParser.nodeAttribute_return retval = new CndParser.nodeAttribute_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        CndParser.autoCreated_return autoCreated104 = null;

        CndParser.mandatory_return mandatory105 = null;

        CndParser.isPrimary_return isPrimary106 = null;

        CndParser.isProtected_return isProtected107 = null;

        CndParser.onParentVersioning_return onParentVersioning108 = null;

        CndParser.sns_return sns109 = null;


        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:121:15: ( autoCreated | mandatory | ( isPrimary )=> isPrimary | ( isProtected )=> isProtected | onParentVersioning | sns )
            int alt31 = 6;
            alt31 = dfa31.predict(input);
            switch (alt31) {
                case 1:
                    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:121:17: autoCreated
                {
                    root_0 = (Object) adaptor.nil();

                    pushFollow(FOLLOW_autoCreated_in_nodeAttribute1262);
                    autoCreated104 = autoCreated();

                    state._fsp--;
                    if (state.failed) return retval;
                    if (state.backtracking == 0) adaptor.addChild(root_0, autoCreated104.getTree());

                }
                break;
                case 2:
                    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:121:31: mandatory
                {
                    root_0 = (Object) adaptor.nil();

                    pushFollow(FOLLOW_mandatory_in_nodeAttribute1266);
                    mandatory105 = mandatory();

                    state._fsp--;
                    if (state.failed) return retval;
                    if (state.backtracking == 0) adaptor.addChild(root_0, mandatory105.getTree());

                }
                break;
                case 3:
                    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:121:43: ( isPrimary )=> isPrimary
                {
                    root_0 = (Object) adaptor.nil();

                    pushFollow(FOLLOW_isPrimary_in_nodeAttribute1274);
                    isPrimary106 = isPrimary();

                    state._fsp--;
                    if (state.failed) return retval;
                    if (state.backtracking == 0) adaptor.addChild(root_0, isPrimary106.getTree());

                }
                break;
                case 4:
                    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:121:67: ( isProtected )=> isProtected
                {
                    root_0 = (Object) adaptor.nil();

                    pushFollow(FOLLOW_isProtected_in_nodeAttribute1281);
                    isProtected107 = isProtected();

                    state._fsp--;
                    if (state.failed) return retval;
                    if (state.backtracking == 0) adaptor.addChild(root_0, isProtected107.getTree());

                }
                break;
                case 5:
                    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:121:96: onParentVersioning
                {
                    root_0 = (Object) adaptor.nil();

                    pushFollow(FOLLOW_onParentVersioning_in_nodeAttribute1285);
                    onParentVersioning108 = onParentVersioning();

                    state._fsp--;
                    if (state.failed) return retval;
                    if (state.backtracking == 0) adaptor.addChild(root_0, onParentVersioning108.getTree());

                }
                break;
                case 6:
                    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:121:117: sns
                {
                    root_0 = (Object) adaptor.nil();

                    pushFollow(FOLLOW_sns_in_nodeAttribute1289);
                    sns109 = sns();

                    state._fsp--;
                    if (state.failed) return retval;
                    if (state.backtracking == 0) adaptor.addChild(root_0, sns109.getTree());

                }
                break;

            }
            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "nodeAttribute"

    public static class sns_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "sns"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:122:1: sns : ( 'sns' | '*' | 'multiple' ) -> ^( SAME_NAME_SIBLINGS STRING[\"true\"] ) ;
    public final CndParser.sns_return sns() throws RecognitionException {
        CndParser.sns_return retval = new CndParser.sns_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token string_literal110 = null;
        Token char_literal111 = null;
        Token string_literal112 = null;

        Object string_literal110_tree = null;
        Object char_literal111_tree = null;
        Object string_literal112_tree = null;
        RewriteRuleTokenStream stream_93 = new RewriteRuleTokenStream(adaptor, "token 93");
        RewriteRuleTokenStream stream_61 = new RewriteRuleTokenStream(adaptor, "token 61");
        RewriteRuleTokenStream stream_101 = new RewriteRuleTokenStream(adaptor, "token 101");

        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:122:5: ( ( 'sns' | '*' | 'multiple' ) -> ^( SAME_NAME_SIBLINGS STRING[\"true\"] ) )
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:122:7: ( 'sns' | '*' | 'multiple' )
            {
                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:122:7: ( 'sns' | '*' | 'multiple' )
                int alt32 = 3;
                switch (input.LA(1)) {
                    case 101: {
                        alt32 = 1;
                    }
                    break;
                    case 61: {
                        alt32 = 2;
                    }
                    break;
                    case 93: {
                        alt32 = 3;
                    }
                    break;
                    default:
                        if (state.backtracking > 0) {
                            state.failed = true;
                            return retval;
                        }
                        NoViableAltException nvae =
                                new NoViableAltException("", 32, 0, input);

                        throw nvae;
                }

                switch (alt32) {
                    case 1:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:122:8: 'sns'
                    {
                        string_literal110 = (Token) match(input, 101, FOLLOW_101_in_sns1298);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_101.add(string_literal110);


                    }
                    break;
                    case 2:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:122:14: '*'
                    {
                        char_literal111 = (Token) match(input, 61, FOLLOW_61_in_sns1300);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_61.add(char_literal111);


                    }
                    break;
                    case 3:
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:122:18: 'multiple'
                    {
                        string_literal112 = (Token) match(input, 93, FOLLOW_93_in_sns1302);
                        if (state.failed) return retval;
                        if (state.backtracking == 0) stream_93.add(string_literal112);


                    }
                    break;

                }


                // AST REWRITE
                // elements: 
                // token labels: 
                // rule labels: retval
                // token list labels: 
                // rule list labels: 
                // wildcard labels: 
                if (state.backtracking == 0) {
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "rule retval", retval != null ? retval.tree : null);

                    root_0 = (Object) adaptor.nil();
                    // 122:30: -> ^( SAME_NAME_SIBLINGS STRING[\"true\"] )
                    {
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:122:33: ^( SAME_NAME_SIBLINGS STRING[\"true\"] )
                        {
                            Object root_1 = (Object) adaptor.nil();
                            root_1 = (Object) adaptor.becomeRoot((Object) adaptor.create(SAME_NAME_SIBLINGS, "SAME_NAME_SIBLINGS"), root_1);

                            adaptor.addChild(root_1, (Object) adaptor.create(STRING, "true"));

                            adaptor.addChild(root_0, root_1);
                        }

                    }

                    retval.tree = root_0;
                }
            }

            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "sns"

    public static class stringList_return extends ParserRuleReturnScope {
        Object tree;

        public Object getTree() {
            return tree;
        }
    }

    ;

    // $ANTLR start "stringList"
    // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:125:1: stringList : STRING ( ',' STRING )* -> ( STRING )* ;
    public final CndParser.stringList_return stringList() throws RecognitionException {
        CndParser.stringList_return retval = new CndParser.stringList_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token STRING113 = null;
        Token char_literal114 = null;
        Token STRING115 = null;

        Object STRING113_tree = null;
        Object char_literal114_tree = null;
        Object STRING115_tree = null;
        RewriteRuleTokenStream stream_102 = new RewriteRuleTokenStream(adaptor, "token 102");
        RewriteRuleTokenStream stream_STRING = new RewriteRuleTokenStream(adaptor, "token STRING");

        try {
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:125:12: ( STRING ( ',' STRING )* -> ( STRING )* )
            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:125:14: STRING ( ',' STRING )*
            {
                STRING113 = (Token) match(input, STRING, FOLLOW_STRING_in_stringList1322);
                if (state.failed) return retval;
                if (state.backtracking == 0) stream_STRING.add(STRING113);

                // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:125:21: ( ',' STRING )*
                loop33:
                do {
                    int alt33 = 2;
                    int LA33_0 = input.LA(1);

                    if ((LA33_0 == 102)) {
                        alt33 = 1;
                    }


                    switch (alt33) {
                        case 1:
                            // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:125:22: ',' STRING
                        {
                            char_literal114 = (Token) match(input, 102, FOLLOW_102_in_stringList1325);
                            if (state.failed) return retval;
                            if (state.backtracking == 0) stream_102.add(char_literal114);

                            STRING115 = (Token) match(input, STRING, FOLLOW_STRING_in_stringList1327);
                            if (state.failed) return retval;
                            if (state.backtracking == 0) stream_STRING.add(STRING115);


                        }
                        break;

                        default:
                            break loop33;
                    }
                } while (true);


                // AST REWRITE
                // elements: STRING
                // token labels: 
                // rule labels: retval
                // token list labels: 
                // rule list labels: 
                // wildcard labels: 
                if (state.backtracking == 0) {
                    retval.tree = root_0;
                    RewriteRuleSubtreeStream stream_retval = new RewriteRuleSubtreeStream(adaptor, "rule retval", retval != null ? retval.tree : null);

                    root_0 = (Object) adaptor.nil();
                    // 125:36: -> ( STRING )*
                    {
                        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:125:39: ( STRING )*
                        while (stream_STRING.hasNext()) {
                            adaptor.addChild(root_0, stream_STRING.nextNode());

                        }
                        stream_STRING.reset();

                    }

                    retval.tree = root_0;
                }
            }

            retval.stop = input.LT(-1);

            if (state.backtracking == 0) {

                retval.tree = (Object) adaptor.rulePostProcessing(root_0);
                adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        } catch (RecognitionException re) {
            reportError(re);
        } finally {
        }
        return retval;
    }
    // $ANTLR end "stringList"

    // $ANTLR start synpred1_Cnd
    public final void synpred1_Cnd_fragment() throws RecognitionException {
        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:97:24: ( isPrimary )
        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:97:25: isPrimary
        {
            pushFollow(FOLLOW_isPrimary_in_synpred1_Cnd823);
            isPrimary();

            state._fsp--;
            if (state.failed) return;

        }
    }
    // $ANTLR end synpred1_Cnd

    // $ANTLR start synpred2_Cnd
    public final void synpred2_Cnd_fragment() throws RecognitionException {
        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:97:49: ( onParentVersioningLiteral )
        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:97:50: onParentVersioningLiteral
        {
            pushFollow(FOLLOW_onParentVersioningLiteral_in_synpred2_Cnd831);
            onParentVersioningLiteral();

            state._fsp--;
            if (state.failed) return;

        }
    }
    // $ANTLR end synpred2_Cnd

    // $ANTLR start synpred3_Cnd
    public final void synpred3_Cnd_fragment() throws RecognitionException {
        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:97:99: ( autoCreated )
        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:97:100: autoCreated
        {
            pushFollow(FOLLOW_autoCreated_in_synpred3_Cnd839);
            autoCreated();

            state._fsp--;
            if (state.failed) return;

        }
    }
    // $ANTLR end synpred3_Cnd

    // $ANTLR start synpred4_Cnd
    public final void synpred4_Cnd_fragment() throws RecognitionException {
        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:97:129: ( multiple )
        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:97:130: multiple
        {
            pushFollow(FOLLOW_multiple_in_synpred4_Cnd848);
            multiple();

            state._fsp--;
            if (state.failed) return;

        }
    }
    // $ANTLR end synpred4_Cnd

    // $ANTLR start synpred5_Cnd
    public final void synpred5_Cnd_fragment() throws RecognitionException {
        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:97:152: ( mandatory )
        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:97:153: mandatory
        {
            pushFollow(FOLLOW_mandatory_in_synpred5_Cnd856);
            mandatory();

            state._fsp--;
            if (state.failed) return;

        }
    }
    // $ANTLR end synpred5_Cnd

    // $ANTLR start synpred6_Cnd
    public final void synpred6_Cnd_fragment() throws RecognitionException {
        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:97:177: ( isProtected )
        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:97:178: isProtected
        {
            pushFollow(FOLLOW_isProtected_in_synpred6_Cnd864);
            isProtected();

            state._fsp--;
            if (state.failed) return;

        }
    }
    // $ANTLR end synpred6_Cnd

    // $ANTLR start synpred7_Cnd
    public final void synpred7_Cnd_fragment() throws RecognitionException {
        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:97:206: ( queryOperators )
        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:97:207: queryOperators
        {
            pushFollow(FOLLOW_queryOperators_in_synpred7_Cnd872);
            queryOperators();

            state._fsp--;
            if (state.failed) return;

        }
    }
    // $ANTLR end synpred7_Cnd

    // $ANTLR start synpred8_Cnd
    public final void synpred8_Cnd_fragment() throws RecognitionException {
        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:97:241: ( noFullText )
        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:97:242: noFullText
        {
            pushFollow(FOLLOW_noFullText_in_synpred8_Cnd880);
            noFullText();

            state._fsp--;
            if (state.failed) return;

        }
    }
    // $ANTLR end synpred8_Cnd

    // $ANTLR start synpred9_Cnd
    public final void synpred9_Cnd_fragment() throws RecognitionException {
        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:97:268: ( noQueryOrder )
        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:97:269: noQueryOrder
        {
            pushFollow(FOLLOW_noQueryOrder_in_synpred9_Cnd888);
            noQueryOrder();

            state._fsp--;
            if (state.failed) return;

        }
    }
    // $ANTLR end synpred9_Cnd

    // $ANTLR start synpred10_Cnd
    public final void synpred10_Cnd_fragment() throws RecognitionException {
        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:121:43: ( isPrimary )
        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:121:44: isPrimary
        {
            pushFollow(FOLLOW_isPrimary_in_synpred10_Cnd1271);
            isPrimary();

            state._fsp--;
            if (state.failed) return;

        }
    }
    // $ANTLR end synpred10_Cnd

    // $ANTLR start synpred11_Cnd
    public final void synpred11_Cnd_fragment() throws RecognitionException {
        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:121:67: ( isProtected )
        // /home/machak/java/projects/grammar-parser/src/main/resources/Cnd.g:121:68: isProtected
        {
            pushFollow(FOLLOW_isProtected_in_synpred11_Cnd1278);
            isProtected();

            state._fsp--;
            if (state.failed) return;

        }
    }
    // $ANTLR end synpred11_Cnd

    // Delegated rules

    public final boolean synpred2_Cnd() {
        state.backtracking++;
        int start = input.mark();
        try {
            synpred2_Cnd_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: " + re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        state.backtracking--;
        state.failed = false;
        return success;
    }

    public final boolean synpred8_Cnd() {
        state.backtracking++;
        int start = input.mark();
        try {
            synpred8_Cnd_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: " + re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        state.backtracking--;
        state.failed = false;
        return success;
    }

    public final boolean synpred5_Cnd() {
        state.backtracking++;
        int start = input.mark();
        try {
            synpred5_Cnd_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: " + re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        state.backtracking--;
        state.failed = false;
        return success;
    }

    public final boolean synpred6_Cnd() {
        state.backtracking++;
        int start = input.mark();
        try {
            synpred6_Cnd_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: " + re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        state.backtracking--;
        state.failed = false;
        return success;
    }

    public final boolean synpred11_Cnd() {
        state.backtracking++;
        int start = input.mark();
        try {
            synpred11_Cnd_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: " + re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        state.backtracking--;
        state.failed = false;
        return success;
    }

    public final boolean synpred7_Cnd() {
        state.backtracking++;
        int start = input.mark();
        try {
            synpred7_Cnd_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: " + re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        state.backtracking--;
        state.failed = false;
        return success;
    }

    public final boolean synpred4_Cnd() {
        state.backtracking++;
        int start = input.mark();
        try {
            synpred4_Cnd_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: " + re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        state.backtracking--;
        state.failed = false;
        return success;
    }

    public final boolean synpred9_Cnd() {
        state.backtracking++;
        int start = input.mark();
        try {
            synpred9_Cnd_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: " + re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        state.backtracking--;
        state.failed = false;
        return success;
    }

    public final boolean synpred1_Cnd() {
        state.backtracking++;
        int start = input.mark();
        try {
            synpred1_Cnd_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: " + re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        state.backtracking--;
        state.failed = false;
        return success;
    }

    public final boolean synpred3_Cnd() {
        state.backtracking++;
        int start = input.mark();
        try {
            synpred3_Cnd_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: " + re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        state.backtracking--;
        state.failed = false;
        return success;
    }

    public final boolean synpred10_Cnd() {
        state.backtracking++;
        int start = input.mark();
        try {
            synpred10_Cnd_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: " + re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        state.backtracking--;
        state.failed = false;
        return success;
    }


    protected DFA17 dfa17 = new DFA17(this);
    protected DFA31 dfa31 = new DFA31(this);
    static final String DFA17_eotS =
            "\41\uffff";
    static final String DFA17_eofS =
            "\1\1\40\uffff";
    static final String DFA17_minS =
            "\1\52\1\uffff\26\0\11\uffff";
    static final String DFA17_maxS =
            "\1\144\1\uffff\26\0\11\uffff";
    static final String DFA17_acceptS =
            "\1\uffff\1\12\26\uffff\1\1\1\2\1\3\1\4\1\5\1\6\1\7\1\10\1\11";
    static final String DFA17_specialS =
            "\2\uffff\1\11\1\13\1\5\1\4\1\16\1\12\1\10\1\3\1\15\1\21\1\2\1\6" +
                    "\1\7\1\1\1\0\1\17\1\22\1\23\1\14\1\20\1\24\1\25\11\uffff}>";
    static final String[] DFA17_transitionS = {
            "\1\1\3\uffff\1\1\4\uffff\1\14\1\uffff\1\6\5\uffff\1\2\1\1\1" +
                    "\11\17\uffff\1\3\1\4\1\7\1\10\1\15\1\16\1\17\1\20\1\21\6\5\1" +
                    "\12\1\13\1\24\1\25\1\26\1\27\1\22\1\23\1\1",
            "",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            ""
    };

    static final short[] DFA17_eot = DFA.unpackEncodedString(DFA17_eotS);
    static final short[] DFA17_eof = DFA.unpackEncodedString(DFA17_eofS);
    static final char[] DFA17_min = DFA.unpackEncodedStringToUnsignedChars(DFA17_minS);
    static final char[] DFA17_max = DFA.unpackEncodedStringToUnsignedChars(DFA17_maxS);
    static final short[] DFA17_accept = DFA.unpackEncodedString(DFA17_acceptS);
    static final short[] DFA17_special = DFA.unpackEncodedString(DFA17_specialS);
    static final short[][] DFA17_transition;

    static {
        int numStates = DFA17_transitionS.length;
        DFA17_transition = new short[numStates][];
        for (int i = 0; i < numStates; i++) {
            DFA17_transition[i] = DFA.unpackEncodedString(DFA17_transitionS[i]);
        }
    }

    class DFA17 extends DFA {

        public DFA17(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 17;
            this.eot = DFA17_eot;
            this.eof = DFA17_eof;
            this.min = DFA17_min;
            this.max = DFA17_max;
            this.accept = DFA17_accept;
            this.special = DFA17_special;
            this.transition = DFA17_transition;
        }

        public String getDescription() {
            return "()+ loopback of 97:22: ( ( isPrimary )=> isPrimary | ( onParentVersioningLiteral )=> onParentVersioning | ( autoCreated )=> autoCreated | ( multiple )=> multiple | ( mandatory )=> mandatory | ( isProtected )=> isProtected | ( queryOperators )=> queryOperators | ( noFullText )=> noFullText | ( noQueryOrder )=> noQueryOrder )+";
        }

        public int specialStateTransition(int s, IntStream _input) throws NoViableAltException {
            TokenStream input = (TokenStream) _input;
            int _s = s;
            switch (s) {
                case 0:
                    int LA17_16 = input.LA(1);


                    int index17_16 = input.index();
                    input.rewind();
                    s = -1;
                    if ((synpred6_Cnd())) {
                        s = 29;
                    } else if ((true)) {
                        s = 1;
                    }


                    input.seek(index17_16);
                    if (s >= 0) return s;
                    break;
                case 1:
                    int LA17_15 = input.LA(1);


                    int index17_15 = input.index();
                    input.rewind();
                    s = -1;
                    if ((synpred6_Cnd())) {
                        s = 29;
                    } else if ((true)) {
                        s = 1;
                    }


                    input.seek(index17_15);
                    if (s >= 0) return s;
                    break;
                case 2:
                    int LA17_12 = input.LA(1);


                    int index17_12 = input.index();
                    input.rewind();
                    s = -1;
                    if ((synpred5_Cnd())) {
                        s = 28;
                    } else if ((true)) {
                        s = 1;
                    }


                    input.seek(index17_12);
                    if (s >= 0) return s;
                    break;
                case 3:
                    int LA17_9 = input.LA(1);


                    int index17_9 = input.index();
                    input.rewind();
                    s = -1;
                    if ((synpred4_Cnd())) {
                        s = 27;
                    } else if ((true)) {
                        s = 1;
                    }


                    input.seek(index17_9);
                    if (s >= 0) return s;
                    break;
                case 4:
                    int LA17_5 = input.LA(1);


                    int index17_5 = input.index();
                    input.rewind();
                    s = -1;
                    if ((synpred2_Cnd())) {
                        s = 25;
                    } else if ((true)) {
                        s = 1;
                    }


                    input.seek(index17_5);
                    if (s >= 0) return s;
                    break;
                case 5:
                    int LA17_4 = input.LA(1);


                    int index17_4 = input.index();
                    input.rewind();
                    s = -1;
                    if ((synpred1_Cnd())) {
                        s = 24;
                    } else if ((true)) {
                        s = 1;
                    }


                    input.seek(index17_4);
                    if (s >= 0) return s;
                    break;
                case 6:
                    int LA17_13 = input.LA(1);


                    int index17_13 = input.index();
                    input.rewind();
                    s = -1;
                    if ((synpred5_Cnd())) {
                        s = 28;
                    } else if ((true)) {
                        s = 1;
                    }


                    input.seek(index17_13);
                    if (s >= 0) return s;
                    break;
                case 7:
                    int LA17_14 = input.LA(1);


                    int index17_14 = input.index();
                    input.rewind();
                    s = -1;
                    if ((synpred5_Cnd())) {
                        s = 28;
                    } else if ((true)) {
                        s = 1;
                    }


                    input.seek(index17_14);
                    if (s >= 0) return s;
                    break;
                case 8:
                    int LA17_8 = input.LA(1);


                    int index17_8 = input.index();
                    input.rewind();
                    s = -1;
                    if ((synpred3_Cnd())) {
                        s = 26;
                    } else if ((true)) {
                        s = 1;
                    }


                    input.seek(index17_8);
                    if (s >= 0) return s;
                    break;
                case 9:
                    int LA17_2 = input.LA(1);


                    int index17_2 = input.index();
                    input.rewind();
                    s = -1;
                    if ((synpred1_Cnd())) {
                        s = 24;
                    } else if ((true)) {
                        s = 1;
                    }


                    input.seek(index17_2);
                    if (s >= 0) return s;
                    break;
                case 10:
                    int LA17_7 = input.LA(1);


                    int index17_7 = input.index();
                    input.rewind();
                    s = -1;
                    if ((synpred3_Cnd())) {
                        s = 26;
                    } else if ((true)) {
                        s = 1;
                    }


                    input.seek(index17_7);
                    if (s >= 0) return s;
                    break;
                case 11:
                    int LA17_3 = input.LA(1);


                    int index17_3 = input.index();
                    input.rewind();
                    s = -1;
                    if ((synpred1_Cnd())) {
                        s = 24;
                    } else if ((true)) {
                        s = 1;
                    }


                    input.seek(index17_3);
                    if (s >= 0) return s;
                    break;
                case 12:
                    int LA17_20 = input.LA(1);


                    int index17_20 = input.index();
                    input.rewind();
                    s = -1;
                    if ((synpred8_Cnd())) {
                        s = 31;
                    } else if ((true)) {
                        s = 1;
                    }


                    input.seek(index17_20);
                    if (s >= 0) return s;
                    break;
                case 13:
                    int LA17_10 = input.LA(1);


                    int index17_10 = input.index();
                    input.rewind();
                    s = -1;
                    if ((synpred4_Cnd())) {
                        s = 27;
                    } else if ((true)) {
                        s = 1;
                    }


                    input.seek(index17_10);
                    if (s >= 0) return s;
                    break;
                case 14:
                    int LA17_6 = input.LA(1);


                    int index17_6 = input.index();
                    input.rewind();
                    s = -1;
                    if ((synpred3_Cnd())) {
                        s = 26;
                    } else if ((true)) {
                        s = 1;
                    }


                    input.seek(index17_6);
                    if (s >= 0) return s;
                    break;
                case 15:
                    int LA17_17 = input.LA(1);


                    int index17_17 = input.index();
                    input.rewind();
                    s = -1;
                    if ((synpred6_Cnd())) {
                        s = 29;
                    } else if ((true)) {
                        s = 1;
                    }


                    input.seek(index17_17);
                    if (s >= 0) return s;
                    break;
                case 16:
                    int LA17_21 = input.LA(1);


                    int index17_21 = input.index();
                    input.rewind();
                    s = -1;
                    if ((synpred8_Cnd())) {
                        s = 31;
                    } else if ((true)) {
                        s = 1;
                    }


                    input.seek(index17_21);
                    if (s >= 0) return s;
                    break;
                case 17:
                    int LA17_11 = input.LA(1);


                    int index17_11 = input.index();
                    input.rewind();
                    s = -1;
                    if ((synpred4_Cnd())) {
                        s = 27;
                    } else if ((true)) {
                        s = 1;
                    }


                    input.seek(index17_11);
                    if (s >= 0) return s;
                    break;
                case 18:
                    int LA17_18 = input.LA(1);


                    int index17_18 = input.index();
                    input.rewind();
                    s = -1;
                    if ((synpred7_Cnd())) {
                        s = 30;
                    } else if ((true)) {
                        s = 1;
                    }


                    input.seek(index17_18);
                    if (s >= 0) return s;
                    break;
                case 19:
                    int LA17_19 = input.LA(1);


                    int index17_19 = input.index();
                    input.rewind();
                    s = -1;
                    if ((synpred7_Cnd())) {
                        s = 30;
                    } else if ((true)) {
                        s = 1;
                    }


                    input.seek(index17_19);
                    if (s >= 0) return s;
                    break;
                case 20:
                    int LA17_22 = input.LA(1);


                    int index17_22 = input.index();
                    input.rewind();
                    s = -1;
                    if ((synpred9_Cnd())) {
                        s = 32;
                    } else if ((true)) {
                        s = 1;
                    }


                    input.seek(index17_22);
                    if (s >= 0) return s;
                    break;
                case 21:
                    int LA17_23 = input.LA(1);


                    int index17_23 = input.index();
                    input.rewind();
                    s = -1;
                    if ((synpred9_Cnd())) {
                        s = 32;
                    } else if ((true)) {
                        s = 1;
                    }


                    input.seek(index17_23);
                    if (s >= 0) return s;
                    break;
            }
            if (state.backtracking > 0) {
                state.failed = true;
                return -1;
            }
            NoViableAltException nvae =
                    new NoViableAltException(getDescription(), 17, _s, input);
            error(nvae);
            throw nvae;
        }
    }

    static final String DFA31_eotS =
            "\13\uffff";
    static final String DFA31_eofS =
            "\13\uffff";
    static final String DFA31_minS =
            "\1\63\12\uffff";
    static final String DFA31_maxS =
            "\1\145\12\uffff";
    static final String DFA31_acceptS =
            "\1\uffff\1\1\1\2\3\3\3\4\1\5\1\6";
    static final String DFA31_specialS =
            "\1\0\12\uffff}>";
    static final String[] DFA31_transitionS = {
            "\1\2\1\uffff\1\1\5\uffff\1\3\1\uffff\1\12\17\uffff\1\4\1\5\2" +
                    "\1\2\2\1\6\1\7\1\10\6\11\1\uffff\1\12\7\uffff\1\12",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            ""
    };

    static final short[] DFA31_eot = DFA.unpackEncodedString(DFA31_eotS);
    static final short[] DFA31_eof = DFA.unpackEncodedString(DFA31_eofS);
    static final char[] DFA31_min = DFA.unpackEncodedStringToUnsignedChars(DFA31_minS);
    static final char[] DFA31_max = DFA.unpackEncodedStringToUnsignedChars(DFA31_maxS);
    static final short[] DFA31_accept = DFA.unpackEncodedString(DFA31_acceptS);
    static final short[] DFA31_special = DFA.unpackEncodedString(DFA31_specialS);
    static final short[][] DFA31_transition;

    static {
        int numStates = DFA31_transitionS.length;
        DFA31_transition = new short[numStates][];
        for (int i = 0; i < numStates; i++) {
            DFA31_transition[i] = DFA.unpackEncodedString(DFA31_transitionS[i]);
        }
    }

    class DFA31 extends DFA {

        public DFA31(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 31;
            this.eot = DFA31_eot;
            this.eof = DFA31_eof;
            this.min = DFA31_min;
            this.max = DFA31_max;
            this.accept = DFA31_accept;
            this.special = DFA31_special;
            this.transition = DFA31_transition;
        }

        public String getDescription() {
            return "121:1: nodeAttribute : ( autoCreated | mandatory | ( isPrimary )=> isPrimary | ( isProtected )=> isProtected | onParentVersioning | sns );";
        }

        public int specialStateTransition(int s, IntStream _input) throws NoViableAltException {
            TokenStream input = (TokenStream) _input;
            int _s = s;
            switch (s) {
                case 0:
                    int LA31_0 = input.LA(1);


                    int index31_0 = input.index();
                    input.rewind();
                    s = -1;
                    if ((LA31_0 == 53 || (LA31_0 >= 79 && LA31_0 <= 80))) {
                        s = 1;
                    } else if ((LA31_0 == 51 || (LA31_0 >= 81 && LA31_0 <= 82))) {
                        s = 2;
                    } else if ((LA31_0 == 59) && (synpred10_Cnd())) {
                        s = 3;
                    } else if ((LA31_0 == 77) && (synpred10_Cnd())) {
                        s = 4;
                    } else if ((LA31_0 == 78) && (synpred10_Cnd())) {
                        s = 5;
                    } else if ((LA31_0 == 83) && (synpred11_Cnd())) {
                        s = 6;
                    } else if ((LA31_0 == 84) && (synpred11_Cnd())) {
                        s = 7;
                    } else if ((LA31_0 == 85) && (synpred11_Cnd())) {
                        s = 8;
                    } else if (((LA31_0 >= 86 && LA31_0 <= 91))) {
                        s = 9;
                    } else if ((LA31_0 == 61 || LA31_0 == 93 || LA31_0 == 101)) {
                        s = 10;
                    }


                    input.seek(index31_0);
                    if (s >= 0) return s;
                    break;
            }
            if (state.backtracking > 0) {
                state.failed = true;
                return -1;
            }
            NoViableAltException nvae =
                    new NoViableAltException(getDescription(), 31, _s, input);
            error(nvae);
            throw nvae;
        }
    }


    public static final BitSet FOLLOW_namespaceMapping_in_cnd283 = new BitSet(new long[]{0x0000440000000000L});
    public static final BitSet FOLLOW_nodeTypeDefinition_in_cnd285 = new BitSet(new long[]{0x0000440000000000L});
    public static final BitSet FOLLOW_EOF_in_cnd289 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_42_in_namespaceMapping321 = new BitSet(new long[]{0x0000280800000000L});
    public static final BitSet FOLLOW_prefix_in_namespaceMapping323 = new BitSet(new long[]{0x0000080000000000L});
    public static final BitSet FOLLOW_43_in_namespaceMapping325 = new BitSet(new long[]{0x0000000800000000L});
    public static final BitSet FOLLOW_uri_in_namespaceMapping327 = new BitSet(new long[]{0x0000100000000000L});
    public static final BitSet FOLLOW_44_in_namespaceMapping329 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_45_in_prefix350 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_STRING_in_prefix372 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_STRING_in_uri387 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_nodeTypeName_in_nodeTypeDefinition407 = new BitSet(new long[]{0x1FFF300000000002L, 0x0000001000000000L});
    public static final BitSet FOLLOW_supertypes_in_nodeTypeDefinition409 = new BitSet(new long[]{0x1FFF200000000002L, 0x0000001000000000L});
    public static final BitSet FOLLOW_nodeTypeOptions_in_nodeTypeDefinition412 = new BitSet(new long[]{0x1000000000000002L, 0x0000001000000000L});
    public static final BitSet FOLLOW_propertyDefinition_in_nodeTypeDefinition417 = new BitSet(new long[]{0x1000000000000002L, 0x0000001000000000L});
    public static final BitSet FOLLOW_childNodeDefinition_in_nodeTypeDefinition421 = new BitSet(new long[]{0x1000000000000002L, 0x0000001000000000L});
    public static final BitSet FOLLOW_46_in_nodeTypeName470 = new BitSet(new long[]{0x0000000800000000L});
    public static final BitSet FOLLOW_STRING_in_nodeTypeName472 = new BitSet(new long[]{0x0000800000000000L});
    public static final BitSet FOLLOW_47_in_nodeTypeName474 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_44_in_supertypes489 = new BitSet(new long[]{0x0000000800000000L});
    public static final BitSet FOLLOW_stringList_in_supertypes491 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_nodeTypeOption_in_nodeTypeOptions508 = new BitSet(new long[]{0x0FFF200000000002L});
    public static final BitSet FOLLOW_orderable_in_nodeTypeOption519 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_mixin_in_nodeTypeOption523 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_isAbstract_in_nodeTypeOption527 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_noQuery_in_nodeTypeOption531 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_primaryItem_in_nodeTypeOption535 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_48_in_orderable544 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_49_in_orderable546 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_50_in_orderable548 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_51_in_mixin566 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_45_in_mixin570 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_52_in_mixin574 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_53_in_isAbstract595 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_54_in_isAbstract597 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_55_in_isAbstract599 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_56_in_noQuery619 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_57_in_noQuery621 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_58_in_primaryItem639 = new BitSet(new long[]{0x0000000800000000L});
    public static final BitSet FOLLOW_59_in_primaryItem641 = new BitSet(new long[]{0x0000000800000000L});
    public static final BitSet FOLLOW_STRING_in_primaryItem646 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_propertyName_in_propertyDefinition663 = new BitSet(new long[]{0x68280C0000000002L, 0x0000000FFFFFE000L});
    public static final BitSet FOLLOW_propertyType_in_propertyDefinition665 = new BitSet(new long[]{0x28280C0000000002L, 0x0000000FFFFFE000L});
    public static final BitSet FOLLOW_defaultValues_in_propertyDefinition668 = new BitSet(new long[]{0x2828040000000002L, 0x0000000FFFFFE000L});
    public static final BitSet FOLLOW_propertyAttributes_in_propertyDefinition673 = new BitSet(new long[]{0x2828040000000002L, 0x0000000FFFFFE000L});
    public static final BitSet FOLLOW_valueConstraints_in_propertyDefinition677 = new BitSet(new long[]{0x2828040000000002L, 0x0000000FFFFFE000L});
    public static final BitSet FOLLOW_60_in_propertyName718 = new BitSet(new long[]{0x2000000800000000L});
    public static final BitSet FOLLOW_61_in_propertyName721 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_STRING_in_propertyName733 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_62_in_propertyType749 = new BitSet(new long[]{0x2000000000000000L, 0x0000000000001FFFL});
    public static final BitSet FOLLOW_propertyTypeLiteral_in_propertyType751 = new BitSet(new long[]{0x8000000000000000L});
    public static final BitSet FOLLOW_63_in_propertyType753 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_set_in_propertyTypeLiteral768 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_43_in_defaultValues803 = new BitSet(new long[]{0x0000000800000000L});
    public static final BitSet FOLLOW_stringList_in_defaultValues805 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_isPrimary_in_propertyAttributes826 = new BitSet(new long[]{0x2828000000000002L, 0x0000000FFFFFE000L});
    public static final BitSet FOLLOW_onParentVersioning_in_propertyAttributes834 = new BitSet(new long[]{0x2828000000000002L, 0x0000000FFFFFE000L});
    public static final BitSet FOLLOW_autoCreated_in_propertyAttributes843 = new BitSet(new long[]{0x2828000000000002L, 0x0000000FFFFFE000L});
    public static final BitSet FOLLOW_multiple_in_propertyAttributes851 = new BitSet(new long[]{0x2828000000000002L, 0x0000000FFFFFE000L});
    public static final BitSet FOLLOW_mandatory_in_propertyAttributes859 = new BitSet(new long[]{0x2828000000000002L, 0x0000000FFFFFE000L});
    public static final BitSet FOLLOW_isProtected_in_propertyAttributes867 = new BitSet(new long[]{0x2828000000000002L, 0x0000000FFFFFE000L});
    public static final BitSet FOLLOW_queryOperators_in_propertyAttributes875 = new BitSet(new long[]{0x2828000000000002L, 0x0000000FFFFFE000L});
    public static final BitSet FOLLOW_noFullText_in_propertyAttributes883 = new BitSet(new long[]{0x2828000000000002L, 0x0000000FFFFFE000L});
    public static final BitSet FOLLOW_noQueryOrder_in_propertyAttributes891 = new BitSet(new long[]{0x2828000000000002L, 0x0000000FFFFFE000L});
    public static final BitSet FOLLOW_42_in_valueConstraints902 = new BitSet(new long[]{0x0000000800000000L});
    public static final BitSet FOLLOW_stringList_in_valueConstraints904 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_59_in_isPrimary920 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_77_in_isPrimary922 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_78_in_isPrimary924 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_53_in_autoCreated944 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_79_in_autoCreated946 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_80_in_autoCreated948 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_51_in_mandatory966 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_81_in_mandatory968 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_82_in_mandatory970 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_83_in_isProtected990 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_84_in_isProtected992 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_85_in_isProtected994 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_onParentVersioningLiteral_in_onParentVersioning1014 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_set_in_onParentVersioningLiteral1029 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_61_in_multiple1050 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_92_in_multiple1052 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_93_in_multiple1054 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_94_in_noFullText1075 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_95_in_noFullText1077 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_96_in_noQueryOrder1096 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_97_in_noQueryOrder1098 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_98_in_queryOperators1119 = new BitSet(new long[]{0x0000000800000000L});
    public static final BitSet FOLLOW_99_in_queryOperators1121 = new BitSet(new long[]{0x0000000800000000L});
    public static final BitSet FOLLOW_STRING_in_queryOperators1124 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_nodeName_in_childNodeDefinition1144 = new BitSet(new long[]{0x6828080000000002L, 0x000000202FFFE000L});
    public static final BitSet FOLLOW_requiredTypes_in_childNodeDefinition1146 = new BitSet(new long[]{0x2828080000000002L, 0x000000202FFFE000L});
    public static final BitSet FOLLOW_defaultType_in_childNodeDefinition1149 = new BitSet(new long[]{0x2828000000000002L, 0x000000202FFFE000L});
    public static final BitSet FOLLOW_nodeAttributes_in_childNodeDefinition1152 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_100_in_nodeName1186 = new BitSet(new long[]{0x2000000800000000L});
    public static final BitSet FOLLOW_61_in_nodeName1189 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_STRING_in_nodeName1201 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_62_in_requiredTypes1216 = new BitSet(new long[]{0x0000000800000000L});
    public static final BitSet FOLLOW_stringList_in_requiredTypes1218 = new BitSet(new long[]{0x8000000000000000L});
    public static final BitSet FOLLOW_63_in_requiredTypes1220 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_43_in_defaultType1237 = new BitSet(new long[]{0x0000000800000000L});
    public static final BitSet FOLLOW_STRING_in_defaultType1239 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_nodeAttribute_in_nodeAttributes1254 = new BitSet(new long[]{0x2828000000000002L, 0x000000202FFFE000L});
    public static final BitSet FOLLOW_autoCreated_in_nodeAttribute1262 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_mandatory_in_nodeAttribute1266 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_isPrimary_in_nodeAttribute1274 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_isProtected_in_nodeAttribute1281 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_onParentVersioning_in_nodeAttribute1285 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_sns_in_nodeAttribute1289 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_101_in_sns1298 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_61_in_sns1300 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_93_in_sns1302 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_STRING_in_stringList1322 = new BitSet(new long[]{0x0000000000000002L, 0x0000004000000000L});
    public static final BitSet FOLLOW_102_in_stringList1325 = new BitSet(new long[]{0x0000000800000000L});
    public static final BitSet FOLLOW_STRING_in_stringList1327 = new BitSet(new long[]{0x0000000000000002L, 0x0000004000000000L});
    public static final BitSet FOLLOW_isPrimary_in_synpred1_Cnd823 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_onParentVersioningLiteral_in_synpred2_Cnd831 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_autoCreated_in_synpred3_Cnd839 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_multiple_in_synpred4_Cnd848 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_mandatory_in_synpred5_Cnd856 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_isProtected_in_synpred6_Cnd864 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_queryOperators_in_synpred7_Cnd872 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_noFullText_in_synpred8_Cnd880 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_noQueryOrder_in_synpred9_Cnd888 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_isPrimary_in_synpred10_Cnd1271 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_isProtected_in_synpred11_Cnd1278 = new BitSet(new long[]{0x0000000000000002L});

}